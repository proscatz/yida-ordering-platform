package com.yida.service;

import com.yida.dto.AddressBookCreateDTO;
import com.yida.dto.AddressBookDefaultDTO;
import com.yida.dto.AddressBookUpdateDTO;
import com.yida.entity.AddressBook;
import java.util.List;

public interface AddressBookService {

    List<AddressBook> list();

    Long save(AddressBookCreateDTO dto);

    AddressBook getById(Long id);

    void update(AddressBookUpdateDTO dto);

    void setDefault(AddressBookDefaultDTO dto);

    void deleteById(Long id);

    AddressBook getDefault();

}
