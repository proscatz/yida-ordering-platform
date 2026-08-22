package com.yida.controller.user;

import com.yida.dto.AddressBookCreateDTO;
import com.yida.dto.AddressBookDefaultDTO;
import com.yida.dto.AddressBookUpdateDTO;
import com.yida.entity.AddressBook;
import com.yida.result.Result;
import com.yida.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

@RestController
@RequestMapping("/user/addressBook")
@Api(tags = "用户端地址簿接口")
@Validated
public class AddressBookController {
    private final AddressBookService addressBookService;

    public AddressBookController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @GetMapping("/list")
    @ApiOperation("查询当前用户地址列表")
    public Result<List<AddressBook>> list() {
        return Result.success(addressBookService.list());
    }

    @PostMapping
    @ApiOperation("新增地址")
    public Result<Long> save(@Valid @RequestBody AddressBookCreateDTO dto) {
        return Result.success(addressBookService.save(dto));
    }

    @GetMapping("/{id}")
    @ApiOperation("查询当前用户地址详情")
    public Result<AddressBook> getById(@PathVariable @Positive(message = "地址ID不正确") Long id) {
        return Result.success(addressBookService.getById(id));
    }

    @PutMapping
    @ApiOperation("修改地址")
    public Result<Void> update(@Valid @RequestBody AddressBookUpdateDTO dto) {
        addressBookService.update(dto);
        return Result.success();
    }

    @PutMapping("/default")
    @ApiOperation("设置默认地址")
    public Result<Void> setDefault(@Valid @RequestBody AddressBookDefaultDTO dto) {
        addressBookService.setDefault(dto);
        return Result.success();
    }

    @DeleteMapping
    @ApiOperation("删除地址")
    public Result<Void> deleteById(@RequestParam @Positive(message = "地址ID不正确") Long id) {
        addressBookService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/default")
    @ApiOperation("查询默认地址")
    public Result<AddressBook> getDefault() {
        return Result.success(addressBookService.getDefault());
    }
}
