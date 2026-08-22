package com.yida.service.impl;

import com.yida.constant.MessageConstant;
import com.yida.context.BaseContext;
import com.yida.dto.AddressBookBaseDTO;
import com.yida.dto.AddressBookCreateDTO;
import com.yida.dto.AddressBookDefaultDTO;
import com.yida.dto.AddressBookUpdateDTO;
import com.yida.entity.AddressBook;
import com.yida.exception.AddressBookBusinessException;
import com.yida.mapper.AddressBookMapper;
import com.yida.service.AddressBookService;
import com.yida.service.support.AdministrativeDivisionValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressBookServiceImpl implements AddressBookService {
    private final AddressBookMapper addressBookMapper;
    private final AdministrativeDivisionValidator divisionValidator;

    public AddressBookServiceImpl(AddressBookMapper addressBookMapper,
                                  AdministrativeDivisionValidator divisionValidator) {
        this.addressBookMapper = addressBookMapper;
        this.divisionValidator = divisionValidator;
    }

    @Override
    public List<AddressBook> list() {
        AddressBook condition = new AddressBook();
        condition.setUserId(currentUserId());
        return addressBookMapper.list(condition);
    }

    @Override
    @Transactional
    public Long save(AddressBookCreateDTO dto) {
        divisionValidator.validate(dto);
        Long userId = currentUserId();
        if (dto.getIsDefault() == 1) {
            lockUser(userId);
            addressBookMapper.clearDefaultByUserId(userId);
        }
        AddressBook addressBook = toEntity(dto, null, userId);
        if (addressBookMapper.insert(addressBook) != 1 || addressBook.getId() == null) {
            throw new AddressBookBusinessException("地址保存失败，请稍后重试");
        }
        return addressBook.getId();
    }

    @Override
    public AddressBook getById(Long id) {
        AddressBook addressBook = addressBookMapper.getByIdAndUserId(id, currentUserId());
        if (addressBook == null) {
            throw addressNotFound();
        }
        return addressBook;
    }

    @Override
    @Transactional
    public void update(AddressBookUpdateDTO dto) {
        divisionValidator.validate(dto);
        Long userId = currentUserId();
        if (dto.getIsDefault() == 1) {
            lockUser(userId);
        }
        if (addressBookMapper.getByIdAndUserId(dto.getId(), userId) == null) {
            throw addressNotFound();
        }
        if (dto.getIsDefault() == 1) {
            addressBookMapper.clearDefaultByUserId(userId);
        }
        if (addressBookMapper.update(toEntity(dto, dto.getId(), userId)) != 1) {
            throw addressNotFound();
        }
    }

    @Override
    @Transactional
    public void setDefault(AddressBookDefaultDTO dto) {
        Long userId = currentUserId();
        lockUser(userId);
        if (addressBookMapper.getByIdAndUserId(dto.getId(), userId) == null) {
            throw addressNotFound();
        }
        addressBookMapper.clearDefaultByUserId(userId);
        if (addressBookMapper.setDefaultByIdAndUserId(dto.getId(), userId) != 1) {
            throw addressNotFound();
        }
    }

    @Override
    public void deleteById(Long id) {
        if (addressBookMapper.deleteById(id, currentUserId()) != 1) {
            throw addressNotFound();
        }
    }

    @Override
    public AddressBook getDefault() {
        AddressBook addressBook = addressBookMapper.getDefaultByUserId(currentUserId());
        if (addressBook == null) {
            throw addressNotFound();
        }
        return addressBook;
    }

    private void lockUser(Long userId) {
        if (addressBookMapper.lockUserRow(userId) == null) {
            throw addressNotFound();
        }
    }

    private Long currentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw addressNotFound();
        }
        return userId;
    }

    private AddressBookBusinessException addressNotFound() {
        return new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
    }

    private AddressBook toEntity(AddressBookBaseDTO dto, Long id, Long userId) {
        return AddressBook.builder()
                .id(id)
                .userId(userId)
                .consignee(dto.getConsignee())
                .phone(dto.getPhone())
                .sex(dto.getSex())
                .provinceCode(dto.getProvinceCode())
                .provinceName(dto.getProvinceName())
                .cityCode(dto.getCityCode())
                .cityName(dto.getCityName())
                .districtCode(dto.getDistrictCode())
                .districtName(dto.getDistrictName())
                .detail(dto.getDetail())
                .label(dto.getLabel())
                .isDefault(dto.getIsDefault())
                .build();
    }
}
