package com.yida.service.impl;

import com.yida.context.BaseContext;
import com.yida.dto.AddressBookCreateDTO;
import com.yida.dto.AddressBookDefaultDTO;
import com.yida.dto.AddressBookUpdateDTO;
import com.yida.entity.AddressBook;
import com.yida.exception.AddressBookBusinessException;
import com.yida.mapper.AddressBookMapper;
import com.yida.service.support.AdministrativeDivisionValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static com.yida.dto.AddressBookDtoValidationTest.valid;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddressBookServiceImplTest {
    private AddressBookMapper mapper;
    private AdministrativeDivisionValidator divisionValidator;
    private AddressBookServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(AddressBookMapper.class);
        divisionValidator = mock(AdministrativeDivisionValidator.class);
        service = new AddressBookServiceImpl(mapper, divisionValidator);
        BaseContext.setCurrentId(7L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void legalAddressIsCreatedForAuthenticatedUserAndReturnsId() {
        when(mapper.insert(any())).thenAnswer(invocation -> {
            AddressBook entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        });
        Long id = service.save(valid());
        assertEquals(88L, id);
        ArgumentCaptor<AddressBook> captor = ArgumentCaptor.forClass(AddressBook.class);
        verify(mapper).insert(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        verify(divisionValidator).validate(any());
    }

    @Test
    void newDefaultAddressClearsOldDefaultInsideSameServiceOperation() {
        AddressBookCreateDTO dto = valid();
        dto.setIsDefault(1);
        when(mapper.lockUserRow(7L)).thenReturn(7L);
        when(mapper.insert(any())).thenAnswer(invocation -> {
            ((AddressBook) invocation.getArgument(0)).setId(99L);
            return 1;
        });
        assertEquals(99L, service.save(dto));
        InOrder inOrder = inOrder(mapper);
        inOrder.verify(mapper).lockUserRow(7L);
        inOrder.verify(mapper).clearDefaultByUserId(7L);
        inOrder.verify(mapper).insert(any());
    }

    @Test
    void modifyingAnotherUsersAddressIsRejected() {
        AddressBookUpdateDTO dto = updateDto(55L);
        when(mapper.getByIdAndUserId(55L, 7L)).thenReturn(null);
        assertThrows(AddressBookBusinessException.class, () -> service.update(dto));
        verify(mapper, never()).update(any());
    }

    @Test
    void deletingAnotherUsersAddressIsRejected() {
        when(mapper.deleteById(55L, 7L)).thenReturn(0);
        assertThrows(AddressBookBusinessException.class, () -> service.deleteById(55L));
    }

    @Test
    void settingAnotherUsersAddressAsDefaultIsRejected() {
        AddressBookDefaultDTO dto = defaultDto(55L);
        when(mapper.lockUserRow(7L)).thenReturn(7L);
        when(mapper.getByIdAndUserId(55L, 7L)).thenReturn(null);
        assertThrows(AddressBookBusinessException.class, () -> service.setDefault(dto));
        verify(mapper, never()).clearDefaultByUserId(any());
    }

    @Test
    void listIsAlwaysScopedToAuthenticatedUser() {
        when(mapper.list(any())).thenReturn(List.of());
        service.list();
        ArgumentCaptor<AddressBook> captor = ArgumentCaptor.forClass(AddressBook.class);
        verify(mapper).list(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
    }

    @Test
    void concurrentDefaultChangesAreSerializedByUserRowLock() throws Exception {
        ReentrantLock databaseRowLock = new ReentrantLock();
        AtomicInteger activeCriticalSections = new AtomicInteger();
        AtomicInteger maximumConcurrency = new AtomicInteger();
        AtomicLong finalDefault = new AtomicLong();
        when(mapper.lockUserRow(7L)).thenAnswer(invocation -> {
            databaseRowLock.lock();
            return 7L;
        });
        when(mapper.getByIdAndUserId(anyLong(), eq(7L))).thenAnswer(invocation ->
                AddressBook.builder().id(invocation.getArgument(0)).userId(7L).build());
        when(mapper.clearDefaultByUserId(7L)).thenAnswer(invocation -> {
            int active = activeCriticalSections.incrementAndGet();
            maximumConcurrency.accumulateAndGet(active, Math::max);
            finalDefault.set(0);
            return 1;
        });
        when(mapper.setDefaultByIdAndUserId(anyLong(), eq(7L))).thenAnswer(invocation -> {
            try {
                finalDefault.set(invocation.getArgument(0));
                return 1;
            } finally {
                activeCriticalSections.decrementAndGet();
                databaseRowLock.unlock();
            }
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            executor.submit(() -> setDefaultAfter(start, 101L));
            executor.submit(() -> setDefaultAfter(start, 102L));
            start.countDown();
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(1, maximumConcurrency.get());
        assertTrue(finalDefault.get() == 101L || finalDefault.get() == 102L);
        verify(mapper, times(2)).clearDefaultByUserId(7L);
        verify(mapper, times(2)).setDefaultByIdAndUserId(anyLong(), eq(7L));
    }

    private void setDefaultAfter(CountDownLatch start, long id) {
        try {
            start.await();
            BaseContext.setCurrentId(7L);
            service.setDefault(defaultDto(id));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail(ex);
        } finally {
            BaseContext.removeCurrentId();
        }
    }

    private AddressBookUpdateDTO updateDto(Long id) {
        AddressBookCreateDTO source = valid();
        AddressBookUpdateDTO dto = new AddressBookUpdateDTO();
        dto.setId(id);
        dto.setConsignee(source.getConsignee());
        dto.setPhone(source.getPhone());
        dto.setSex(source.getSex());
        dto.setProvinceCode(source.getProvinceCode());
        dto.setProvinceName(source.getProvinceName());
        dto.setCityCode(source.getCityCode());
        dto.setCityName(source.getCityName());
        dto.setDistrictCode(source.getDistrictCode());
        dto.setDistrictName(source.getDistrictName());
        dto.setDetail(source.getDetail());
        dto.setLabel(source.getLabel());
        dto.setIsDefault(source.getIsDefault());
        return dto;
    }

    private AddressBookDefaultDTO defaultDto(Long id) {
        AddressBookDefaultDTO dto = new AddressBookDefaultDTO();
        dto.setId(id);
        return dto;
    }
}
