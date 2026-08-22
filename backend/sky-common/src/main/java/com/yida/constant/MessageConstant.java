package com.yida.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了菜品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "套餐内包含未启售菜品，无法启售";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String ORDER_REQUEST_ID_REQUIRED = "下单请求号不能为空或格式不正确";
    public static final String ORDER_AMOUNT_ERROR = "订单金额或商品数量异常";
    public static final String ORDER_ITEM_NOT_FOUND = "购物车中的商品不存在";
    public static final String ORDER_SUBMIT_CONFLICT = "订单提交冲突，请使用原请求号重试";
    public static final String PAYMENT_AMOUNT_MISMATCH = "支付金额与本地订单不一致";
    public static final String PAYMENT_METHOD_MISMATCH = "支付方式与本地订单不一致";
    public static final String UPLOAD_FILE_INVALID = "上传文件为空或格式不受支持";
    public static final String UPLOAD_FILE_TOO_LARGE = "上传文件超过大小限制";
    public static final String EMPLOYEE_PERMISSION_DENIED = "当前账号无权管理员工";
    public static final String EMPLOYEE_SELF_STATUS_FORBIDDEN = "不能修改自己的启用状态";
    public static final String ADMIN_STATUS_FORBIDDEN = "管理员账号不能通过员工状态接口修改";
    public static final String EMPLOYEE_STATUS_CONFLICT = "员工状态已发生变化，请刷新后重试";
    public static final String EMPLOYEE_STATUS_INVALID = "员工状态参数不正确";

}
