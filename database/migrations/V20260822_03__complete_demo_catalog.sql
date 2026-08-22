-- 驿达点餐阶段四：补齐菜品与套餐演示数据
-- 目标数据库：Yida
-- 基线：14 个启用菜品分类、28 个用户端可见菜品、3 个启用套餐分类、1 个用户端可见套餐。
-- 预计首次新增：112 个菜品、8 个套餐；重复执行不会重复插入，也不会覆盖已有非空图片或业务字段。

set names utf8mb4;
use Yida;
select database() as stage4_target_database;
start transaction;

set @stage4_operator_id = (
    select id from employee where role = 'ADMIN' and status = 1 order by id limit 1
);

drop temporary table if exists yida_stage4_dish;
create temporary table yida_stage4_dish (
    category_name varchar(64) not null,
    dish_name varchar(128) not null,
    price decimal(10,2) not null,
    image_url varchar(512) not null,
    description_text varchar(255) not null,
    flavor_name varchar(32) null,
    flavor_value varchar(255) null,
    primary key (dish_name)
) engine=InnoDB default charset=utf8mb4;

insert into yida_stage4_dish(category_name,dish_name,price,image_url,description_text,flavor_name,flavor_value) values
('川菜','麻婆豆腐',16.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/1c6cc85ad19747d0b66630309fb0aab5.png','嫩豆腐配郫县豆瓣与花椒，麻辣鲜香','辣度','["不辣","微辣","中辣","特辣"]'),
('川菜','宫保鸡丁',26.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/1c6cc85ad19747d0b66630309fb0aab5.png','鸡丁、花生与脆椒快炒，酸甜微辣','辣度','["不辣","微辣","中辣","特辣"]'),
('川菜','鱼香肉丝',24.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/1c6cc85ad19747d0b66630309fb0aab5.png','肉丝搭配木耳笋丝，经典鱼香风味','辣度','["不辣","微辣","中辣","特辣"]'),
('川菜','回锅肉',29.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/1c6cc85ad19747d0b66630309fb0aab5.png','五花肉与青蒜回锅爆炒，酱香浓郁','辣度','["不辣","微辣","中辣","特辣"]'),
('川菜','辣子鸡丁',32.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/1c6cc85ad19747d0b66630309fb0aab5.png','酥香鸡丁与干辣椒翻炒，香辣下饭','辣度','["不辣","微辣","中辣","特辣"]'),
('川菜','水煮牛肉',38.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/1c6cc85ad19747d0b66630309fb0aab5.png','嫩牛肉配时蔬与花椒辣油','辣度','["不辣","微辣","中辣","特辣"]'),
('川菜','蒜泥白肉',26.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/1c6cc85ad19747d0b66630309fb0aab5.png','薄切白肉淋蒜泥红油，清爽不腻','辣度','["不辣","微辣","中辣","特辣"]'),
('川菜','干煸四季豆',22.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/1c6cc85ad19747d0b66630309fb0aab5.png','四季豆干煸入味，芽菜肉末增香','辣度','["不辣","微辣","中辣","特辣"]'),
('川菜','夫妻肺片',28.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/1c6cc85ad19747d0b66630309fb0aab5.png','卤香牛肉牛杂拌红油与花生碎','辣度','["不辣","微辣","中辣","特辣"]'),
('川菜','酸辣土豆丝',16.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/1c6cc85ad19747d0b66630309fb0aab5.png','爽脆土豆丝，酸辣开胃','辣度','["不辣","微辣","中辣","特辣"]'),
('粤菜','豉汁蒸排骨',32.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/36e0b3e7eca94365bb1f0f39aa0f9573.png','排骨佐豆豉蒸制，咸香软嫩','份量','["标准份","加量"]'),
('粤菜','白切鸡',36.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/36e0b3e7eca94365bb1f0f39aa0f9573.png','鸡肉皮爽肉滑，配姜葱蘸料','份量','["标准份","加量"]'),
('粤菜','菠萝咕咾肉',29.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/36e0b3e7eca94365bb1f0f39aa0f9573.png','酥肉配菠萝彩椒，酸甜可口','份量','["标准份","加量"]'),
('粤菜','蜜汁叉烧',38.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/36e0b3e7eca94365bb1f0f39aa0f9573.png','叉烧蜜香油润，外焦里嫩','份量','["标准份","加量"]'),
('粤菜','上汤娃娃菜',22.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/36e0b3e7eca94365bb1f0f39aa0f9573.png','娃娃菜以清鲜上汤煨制','份量','["标准份","加量"]'),
('粤菜','蚝油牛肉',39.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/36e0b3e7eca94365bb1f0f39aa0f9573.png','嫩牛肉配蚝油快炒，鲜香浓郁','份量','["标准份","加量"]'),
('粤菜','虾仁滑蛋',32.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/36e0b3e7eca94365bb1f0f39aa0f9573.png','鲜虾仁搭配嫩滑鸡蛋','份量','["标准份","加量"]'),
('粤菜','啫啫鸡煲',42.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/36e0b3e7eca94365bb1f0f39aa0f9573.png','鸡块砂锅啫制，酱香扑鼻','份量','["标准份","加量"]'),
('粤菜','清蒸多宝鱼',58.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/36e0b3e7eca94365bb1f0f39aa0f9573.png','多宝鱼清蒸锁鲜，葱油提香','份量','["标准份","加量"]'),
('粤菜','蒜蓉粉丝蒸扇贝',46.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/36e0b3e7eca94365bb1f0f39aa0f9573.png','扇贝配蒜蓉粉丝蒸制','份量','["标准份","加量"]'),
('主食','扬州炒饭',18.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','米饭配虾仁、鸡蛋与时蔬粒炒制','份量','["标准份","加量"]'),
('主食','牛肉炒河粉',22.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','河粉与牛肉大火快炒，锅气十足','份量','["标准份","加量"]'),
('主食','鲜肉水饺',19.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','手工鲜肉水饺，皮薄馅足','份量','["标准份","加量"]'),
('主食','番茄鸡蛋面',18.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','番茄蛋汤搭配爽滑面条','份量','["标准份","加量"]'),
('主食','葱油拌面',16.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','自制葱油拌面，葱香浓郁','份量','["标准份","加量"]'),
('主食','鸡丝凉面',18.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','鸡丝与脆蔬拌面，清爽开胃','份量','["标准份","加量"]'),
('主食','腊味煲仔饭',26.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','腊肠腊肉配煲仔饭与锅巴','份量','["标准份","加量"]'),
('主食','咖喱鸡肉饭',24.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','温和咖喱鸡肉搭配米饭','份量','["标准份","加量"]'),
('主食','香菇卤肉饭',22.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','香菇卤肉浇汁配米饭','份量','["标准份","加量"]'),
('主食','韭菜鸡蛋盒子',15.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','薄皮韭菜鸡蛋馅饼，现烙酥香','份量','["标准份","加量"]'),
('饮品','鲜榨橙汁',12.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e06e2b7e798644e393bdc7c51eaf76ee.png','鲜橙现榨，果香清新','温度','["常温","少冰","正常冰"]'),
('饮品','柠檬蜂蜜茶',10.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e06e2b7e798644e393bdc7c51eaf76ee.png','柠檬与蜂蜜调制，酸甜清爽','温度','["常温","少冰","正常冰"]'),
('饮品','杨梅冰茶',12.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e06e2b7e798644e393bdc7c51eaf76ee.png','杨梅果香融合清茶','温度','["常温","少冰","正常冰"]'),
('饮品','桂花酸梅汤',8.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e06e2b7e798644e393bdc7c51eaf76ee.png','乌梅山楂慢煮，桂花增香','温度','["常温","少冰","正常冰"]'),
('饮品','原味豆浆',6.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e06e2b7e798644e393bdc7c51eaf76ee.png','黄豆现磨，口感醇厚','温度','["常温","少冰","正常冰"]'),
('饮品','椰香拿铁',14.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e06e2b7e798644e393bdc7c51eaf76ee.png','咖啡融合椰香，顺滑不腻','温度','["常温","少冰","正常冰"]'),
('饮品','茉莉绿茶',8.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e06e2b7e798644e393bdc7c51eaf76ee.png','茉莉花香与清爽绿茶','温度','["常温","少冰","正常冰"]'),
('饮品','百香果气泡饮',12.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e06e2b7e798644e393bdc7c51eaf76ee.png','百香果配细腻气泡','温度','["常温","少冰","正常冰"]'),
('饮品','草莓酸奶昔',16.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e06e2b7e798644e393bdc7c51eaf76ee.png','草莓与酸奶打制，浓郁顺滑','温度','["常温","少冰","正常冰"]'),
('饮品','冰糖雪梨',9.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e06e2b7e798644e393bdc7c51eaf76ee.png','雪梨与冰糖炖煮，清甜润口','温度','["常温","少冰","正常冰"]'),
('蜀味烤鱼','香辣鲈鱼烤鱼',68.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/ff9d545530714a0494df843e177d07f1.png','鲈鱼烤制后浸入香辣汤底','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味烤鱼','蒜香清江鱼烤鱼',72.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/ff9d545530714a0494df843e177d07f1.png','清江鱼搭配金蒜汤底','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味烤鱼','青花椒草鱼烤鱼',62.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/ff9d545530714a0494df843e177d07f1.png','草鱼配青花椒，清麻鲜香','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味烤鱼','泡椒江团烤鱼',76.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/ff9d545530714a0494df843e177d07f1.png','江团鱼搭配泡椒酸辣汤底','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味烤鱼','豆豉鮰鱼烤鱼',74.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/ff9d545530714a0494df843e177d07f1.png','鮰鱼配豆豉酱香汤底','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味烤鱼','番茄黑鱼烤鱼',70.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/ff9d545530714a0494df843e177d07f1.png','黑鱼配浓郁番茄汤底','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味烤鱼','酸菜鲈鱼烤鱼',69.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/ff9d545530714a0494df843e177d07f1.png','鲈鱼搭配老坛酸菜汤底','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味牛蛙','紫苏牛蛙煲',58.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c88d5b32c9e24ecda54c5609c8db9b0b.png','牛蛙与紫苏砂锅焖制，香气浓郁','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味牛蛙','双椒爆炒牛蛙',56.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c88d5b32c9e24ecda54c5609c8db9b0b.png','青红双椒爆炒牛蛙，鲜辣爽口','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味牛蛙','泡椒牛蛙',56.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c88d5b32c9e24ecda54c5609c8db9b0b.png','泡椒酸辣入味，牛蛙鲜嫩','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味牛蛙','蒜香牛蛙',58.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c88d5b32c9e24ecda54c5609c8db9b0b.png','金蒜铺满牛蛙，蒜香浓郁','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味牛蛙','麻辣干锅牛蛙',62.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c88d5b32c9e24ecda54c5609c8db9b0b.png','牛蛙与藕片土豆干锅炒制','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味牛蛙','酸汤牛蛙',60.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c88d5b32c9e24ecda54c5609c8db9b0b.png','酸汤开胃，牛蛙细嫩','辣度','["不辣","微辣","中辣","特辣"]'),
('蜀味牛蛙','青花椒牛蛙',60.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c88d5b32c9e24ecda54c5609c8db9b0b.png','青花椒清麻风味，鲜香不腻','辣度','["不辣","微辣","中辣","特辣"]'),
('特色蒸菜','金蒜蒸排骨',32.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/25fb42a1ec774821b09aea3e80e435ed.png','排骨配金蒜蒸制，肉嫩汁香','份量','["标准份","加量"]'),
('特色蒸菜','荷叶粉蒸肉',34.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/25fb42a1ec774821b09aea3e80e435ed.png','五花肉裹米粉荷叶蒸制','份量','["标准份","加量"]'),
('特色蒸菜','香菇蒸滑鸡',30.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/25fb42a1ec774821b09aea3e80e435ed.png','鸡肉与香菇蒸制，鲜嫩多汁','份量','["标准份","加量"]'),
('特色蒸菜','肉末蒸蛋',18.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/25fb42a1ec774821b09aea3e80e435ed.png','嫩滑蒸蛋铺香酥肉末','份量','["标准份","加量"]'),
('特色蒸菜','腊味蒸南瓜',22.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/25fb42a1ec774821b09aea3e80e435ed.png','甜糯南瓜搭配腊味蒸香','份量','["标准份","加量"]'),
('特色蒸菜','豉香蒸凤爪',28.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/25fb42a1ec774821b09aea3e80e435ed.png','凤爪豆豉蒸制，软糯入味','份量','["标准份","加量"]'),
('新鲜时蔬','荷塘小炒',24.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/a1b160e25542488ca7ae34a0232beed2.png','莲藕、荷兰豆与木耳清炒',NULL,NULL),
('新鲜时蔬','蚝油生菜',16.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/a1b160e25542488ca7ae34a0232beed2.png','生菜快炒，蚝油提鲜',NULL,NULL),
('新鲜时蔬','干锅花菜',22.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/a1b160e25542488ca7ae34a0232beed2.png','花菜干锅炒制，香辣脆爽',NULL,NULL),
('新鲜时蔬','上汤西兰花',22.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/a1b160e25542488ca7ae34a0232beed2.png','西兰花以清鲜上汤煨制',NULL,NULL),
('新鲜时蔬','蒜蓉空心菜',18.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/a1b160e25542488ca7ae34a0232beed2.png','空心菜配蒜蓉大火快炒',NULL,NULL),
('新鲜时蔬','白灼菜心',18.80,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/a1b160e25542488ca7ae34a0232beed2.png','菜心白灼，淋豉油葱丝',NULL,NULL),
('水煮鱼','青花椒水煮鱼',58.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/61f8328056bd4037b22406ca0edce20c.png','鱼片配青花椒汤底，清麻鲜香','辣度','["不辣","微辣","中辣","特辣"]'),
('水煮鱼','藤椒黑鱼片',62.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/61f8328056bd4037b22406ca0edce20c.png','黑鱼片搭配藤椒与时蔬','辣度','["不辣","微辣","中辣","特辣"]'),
('水煮鱼','麻辣龙利鱼',56.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/61f8328056bd4037b22406ca0edce20c.png','无刺龙利鱼配麻辣汤底','辣度','["不辣","微辣","中辣","特辣"]'),
('水煮鱼','番茄水煮鱼',58.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/61f8328056bd4037b22406ca0edce20c.png','鱼片配番茄浓汤，酸甜鲜美','辣度','["不辣","微辣","中辣","特辣"]'),
('水煮鱼','金汤酸菜鱼片',60.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/61f8328056bd4037b22406ca0edce20c.png','金汤酸菜搭配嫩滑鱼片','辣度','["不辣","微辣","中辣","特辣"]'),
('水煮鱼','鲜椒鲈鱼片',64.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/61f8328056bd4037b22406ca0edce20c.png','鲈鱼片与鲜椒现煮','辣度','["不辣","微辣","中辣","特辣"]'),
('水煮鱼','豆花水煮鱼',60.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/61f8328056bd4037b22406ca0edce20c.png','嫩豆花搭配麻辣鱼片','辣度','["不辣","微辣","中辣","特辣"]'),
('传统主食','红糖发糕',8.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','红糖发酵米糕，松软香甜','份量','["标准份","加量"]'),
('传统主食','葱油花卷',4.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','手工花卷，葱香层次丰富','份量','["标准份","加量"]'),
('传统主食','鲜肉包',5.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','松软面皮包裹鲜香肉馅','份量','["标准份","加量"]'),
('传统主食','玉米窝窝头',4.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','玉米粗粮制作，清甜扎实','份量','["标准份","加量"]'),
('传统主食','小笼汤包',18.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','薄皮汤包，鲜汁充盈','份量','["标准份","加量"]'),
('传统主食','牛肉馅饼',12.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','现烙牛肉馅饼，外酥里嫩','份量','["标准份","加量"]'),
('传统主食','紫米饭',6.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','紫米与大米蒸制，软糯有嚼劲','份量','["标准份","加量"]'),
('传统主食','红薯杂粮饭',8.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e9ee1537df3a424ea61ff841c429e733.png','红薯搭配多种谷物蒸制','份量','["标准份","加量"]'),
('酒水饮料','冰镇可乐',6.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/567a31f8fd4646cda7e33684b28e8dc1.png','冰爽碳酸饮料','温度','["常温","少冰","正常冰"]'),
('酒水饮料','柠檬苏打水',7.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/567a31f8fd4646cda7e33684b28e8dc1.png','柠檬风味气泡苏打水','温度','["常温","少冰","正常冰"]'),
('酒水饮料','橙味汽水',6.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/567a31f8fd4646cda7e33684b28e8dc1.png','橙香清新的碳酸饮料','温度','["常温","少冰","正常冰"]'),
('酒水饮料','青梅气泡水',8.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/567a31f8fd4646cda7e33684b28e8dc1.png','青梅果香与细腻气泡','温度','["常温","少冰","正常冰"]'),
('酒水饮料','无糖乌龙茶',7.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/567a31f8fd4646cda7e33684b28e8dc1.png','清香乌龙茶，无额外加糖','温度','["常温","少冰","正常冰"]'),
('酒水饮料','蜂蜜柚子茶',9.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/567a31f8fd4646cda7e33684b28e8dc1.png','蜂蜜与柚子果香融合','温度','["常温","少冰","正常冰"]'),
('酒水饮料','纯净水',3.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/567a31f8fd4646cda7e33684b28e8dc1.png','清爽饮用纯净水','温度','["常温","少冰","正常冰"]'),
('汤类','冬瓜丸子汤',18.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/fce0df9c0f3f4d8d94fb15124abb4936.png','冬瓜与手打肉丸清炖','份量','["标准份","加量"]'),
('汤类','菌菇鸡汤',22.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/fce0df9c0f3f4d8d94fb15124abb4936.png','鸡肉与多种菌菇慢炖','份量','["标准份","加量"]'),
('汤类','番茄蛋花汤',14.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/fce0df9c0f3f4d8d94fb15124abb4936.png','番茄酸甜，蛋花细嫩','份量','["标准份","加量"]'),
('汤类','玉米排骨汤',24.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/fce0df9c0f3f4d8d94fb15124abb4936.png','甜玉米与排骨慢炖','份量','["标准份","加量"]'),
('汤类','海带豆腐汤',16.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/fce0df9c0f3f4d8d94fb15124abb4936.png','海带与嫩豆腐清煮','份量','["标准份","加量"]'),
('汤类','莲藕花生汤',20.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/fce0df9c0f3f4d8d94fb15124abb4936.png','莲藕花生炖煮，清甜粉糯','份量','["标准份","加量"]'),
('汤类','丝瓜虾皮汤',16.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/fce0df9c0f3f4d8d94fb15124abb4936.png','丝瓜配虾皮，清鲜爽口','份量','["标准份","加量"]'),
('汤类','萝卜牛腩汤',28.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/fce0df9c0f3f4d8d94fb15124abb4936.png','白萝卜与牛腩慢炖','份量','["标准份","加量"]'),
('驿达招牌饭','照烧鸡腿饭',28.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/af68e647bd354a8ba8a96bbeb63f1003.png','照烧鸡腿搭配米饭与时蔬','米饭','["标准饭","少饭","加饭"]'),
('驿达招牌饭','黑椒牛柳饭',34.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/af68e647bd354a8ba8a96bbeb63f1003.png','黑椒牛柳搭配米饭与彩椒','米饭','["标准饭","少饭","加饭"]'),
('驿达招牌饭','台式卤肉饭',24.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/af68e647bd354a8ba8a96bbeb63f1003.png','卤肉汁浇饭，搭配卤蛋青菜','米饭','["标准饭","少饭","加饭"]'),
('驿达招牌饭','咖喱猪排饭',30.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/af68e647bd354a8ba8a96bbeb63f1003.png','酥脆猪排搭配温和咖喱','米饭','["标准饭","少饭","加饭"]'),
('驿达招牌饭','香菇滑鸡饭',28.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/af68e647bd354a8ba8a96bbeb63f1003.png','香菇滑鸡浇汁配米饭','米饭','["标准饭","少饭","加饭"]'),
('驿达招牌饭','蒜香排骨饭',32.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/af68e647bd354a8ba8a96bbeb63f1003.png','蒜香排骨搭配米饭与时蔬','米饭','["标准饭","少饭","加饭"]'),
('驿达招牌饭','茄汁肉丸饭',26.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/af68e647bd354a8ba8a96bbeb63f1003.png','酸甜茄汁肉丸搭配米饭','米饭','["标准饭","少饭","加饭"]'),
('驿达招牌饭','梅菜扣肉饭',30.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/af68e647bd354a8ba8a96bbeb63f1003.png','梅菜扣肉搭配米饭，咸香下饭','米饭','["标准饭","少饭","加饭"]'),
('轻食能量碗','香煎三文鱼能量碗',42.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e6cfcdf258a7499783ae98dd2733fd97.png','三文鱼、谷物与时蔬组合','酱汁','["油醋汁","芝麻汁","不加酱"]'),
('轻食能量碗','蜜汁鸡腿谷物碗',32.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e6cfcdf258a7499783ae98dd2733fd97.png','蜜汁鸡腿搭配糙米和时蔬','酱汁','["油醋汁","芝麻汁","不加酱"]'),
('轻食能量碗','虾仁藜麦沙拉',36.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e6cfcdf258a7499783ae98dd2733fd97.png','虾仁、藜麦与混合生菜','酱汁','["油醋汁","芝麻汁","不加酱"]'),
('轻食能量碗','牛油果鸡蛋沙拉',30.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e6cfcdf258a7499783ae98dd2733fd97.png','牛油果、鸡蛋与新鲜蔬菜','酱汁','["油醋汁","芝麻汁","不加酱"]'),
('轻食能量碗','香烤南瓜鹰嘴豆碗',28.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e6cfcdf258a7499783ae98dd2733fd97.png','烤南瓜与鹰嘴豆谷物组合','酱汁','["油醋汁","芝麻汁","不加酱"]'),
('轻食能量碗','日式豆腐杂蔬碗',26.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e6cfcdf258a7499783ae98dd2733fd97.png','嫩豆腐搭配杂蔬与糙米','酱汁','["油醋汁","芝麻汁","不加酱"]'),
('轻食能量碗','烟熏鸡胸全麦沙拉',30.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e6cfcdf258a7499783ae98dd2733fd97.png','烟熏鸡胸搭配全麦与生菜','酱汁','["油醋汁","芝麻汁","不加酱"]'),
('轻食能量碗','金枪鱼玉米能量碗',34.00,'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e6cfcdf258a7499783ae98dd2733fd97.png','金枪鱼、玉米与谷物时蔬组合','酱汁','["油醋汁","芝麻汁","不加酱"]');

insert into dish(name,category_id,price,image,description,status,create_time,update_time,create_user,update_user)
select t.dish_name,c.id,t.price,t.image_url,t.description_text,1,now(),now(),@stage4_operator_id,@stage4_operator_id
from yida_stage4_dish t
join category c on c.name=t.category_name and c.type=1
left join dish existing on existing.name=t.dish_name
where existing.id is null;
set @stage4_added_dishes = row_count();

insert into dish_flavor(dish_id,name,value)
select d.id,t.flavor_name,t.flavor_value
from yida_stage4_dish t
join dish d on d.name=t.dish_name
where t.flavor_name is not null
  and not exists (
      select 1 from dish_flavor f where f.dish_id=d.id and f.name=t.flavor_name
  );
set @stage4_added_flavors = row_count();

-- 仅修复上一演示脚本创建且图片为空的记录，不覆盖已有图片。
update dish
set image=case name
    when '驿达香煎鸡排饭' then 'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/af68e647bd354a8ba8a96bbeb63f1003.png'
    when '番茄时蔬意面' then 'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/af68e647bd354a8ba8a96bbeb63f1003.png'
    when '黑椒牛肉能量碗' then 'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e6cfcdf258a7499783ae98dd2733fd97.png'
    when '低脂鸡胸沙拉' then 'https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/e6cfcdf258a7499783ae98dd2733fd97.png'
    else image end,
    update_time=now(),
    update_user=@stage4_operator_id
where name in ('驿达香煎鸡排饭','番茄时蔬意面','黑椒牛肉能量碗','低脂鸡胸沙拉')
  and (image is null or trim(image)='');
set @stage4_repaired_dish_images = row_count();

drop temporary table if exists yida_stage4_setmeal;
create temporary table yida_stage4_setmeal (
    category_name varchar(64) not null,
    setmeal_name varchar(128) not null,
    price decimal(10,2) not null,
    description_text varchar(255) not null,
    image_url varchar(512) not null,
    primary key (setmeal_name)
) engine=InnoDB default charset=utf8mb4;

insert into yida_stage4_setmeal(category_name,setmeal_name,price,description_text,image_url) values
('人气套餐','人气单人鸡腿饭套餐',36.80,'照烧鸡腿饭搭配汤品与清爽饮料','https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c8ef02cf619045ed93d74d5eef425499.png'),
('人气套餐','人气双拼川味套餐',58.80,'宫保鸡丁、麻婆豆腐与主食饮品组合','https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c8ef02cf619045ed93d74d5eef425499.png'),
('人气套餐','人气烤鱼分享套餐',88.80,'香辣烤鱼搭配时蔬、米饭和饮品','https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c8ef02cf619045ed93d74d5eef425499.png'),
('商务套餐','商务轻享鸡胸套餐',42.80,'低脂鸡胸沙拉搭配菌菇鸡汤和清茶','https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c8ef02cf619045ed93d74d5eef425499.png'),
('商务套餐','商务黑椒牛柳套餐',45.80,'黑椒牛柳饭搭配豆腐汤和无糖茶','https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c8ef02cf619045ed93d74d5eef425499.png'),
('商务套餐','商务粤味蒸品套餐',49.80,'香菇蒸滑鸡搭配菜心、米饭和饮品','https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c8ef02cf619045ed93d74d5eef425499.png'),
('驿达预约套餐','驿达家庭三人预约套餐',138.00,'烤鱼、时蔬、汤品与三人主食饮品组合','https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c8ef02cf619045ed93d74d5eef425499.png'),
('驿达预约套餐','驿达轻食双人预约套餐',78.00,'三文鱼能量碗、虾仁藜麦沙拉与双份饮品','https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c8ef02cf619045ed93d74d5eef425499.png');

insert into setmeal(category_id,name,price,status,description,image,create_time,update_time,create_user,update_user)
select c.id,t.setmeal_name,t.price,1,t.description_text,t.image_url,now(),now(),@stage4_operator_id,@stage4_operator_id
from yida_stage4_setmeal t
join category c on c.name=t.category_name and c.type=2
left join setmeal existing on existing.name=t.setmeal_name
where existing.id is null;
set @stage4_added_setmeals = row_count();

update setmeal
set image='https://sky-take-out-32.oss-cn-beijing.aliyuncs.com/catalog/2026/08/22/c8ef02cf619045ed93d74d5eef425499.png',
    update_time=now(),
    update_user=@stage4_operator_id
where name='驿达双人预约套餐'
  and (image is null or trim(image)='');
set @stage4_repaired_setmeal_images = row_count();

drop temporary table if exists yida_stage4_setmeal_dish;
create temporary table yida_stage4_setmeal_dish (
    setmeal_name varchar(128) not null,
    dish_name varchar(128) not null,
    copies int not null,
    primary key (setmeal_name,dish_name)
) engine=InnoDB default charset=utf8mb4;

insert into yida_stage4_setmeal_dish(setmeal_name,dish_name,copies) values
('人气单人鸡腿饭套餐','照烧鸡腿饭',1),
('人气单人鸡腿饭套餐','番茄蛋花汤',1),
('人气单人鸡腿饭套餐','柠檬苏打水',1),
('人气双拼川味套餐','宫保鸡丁',1),
('人气双拼川味套餐','麻婆豆腐',1),
('人气双拼川味套餐','米饭',2),
('人气双拼川味套餐','桂花酸梅汤',2),
('人气烤鱼分享套餐','香辣鲈鱼烤鱼',1),
('人气烤鱼分享套餐','蒜蓉娃娃菜',1),
('人气烤鱼分享套餐','米饭',2),
('人气烤鱼分享套餐','柠檬苏打水',2),
('商务轻享鸡胸套餐','低脂鸡胸沙拉',1),
('商务轻享鸡胸套餐','菌菇鸡汤',1),
('商务轻享鸡胸套餐','茉莉绿茶',1),
('商务黑椒牛柳套餐','黑椒牛柳饭',1),
('商务黑椒牛柳套餐','海带豆腐汤',1),
('商务黑椒牛柳套餐','无糖乌龙茶',1),
('商务粤味蒸品套餐','香菇蒸滑鸡',1),
('商务粤味蒸品套餐','白灼菜心',1),
('商务粤味蒸品套餐','米饭',1),
('商务粤味蒸品套餐','冰糖雪梨',1),
('驿达家庭三人预约套餐','蒜香清江鱼烤鱼',1),
('驿达家庭三人预约套餐','荷塘小炒',1),
('驿达家庭三人预约套餐','玉米排骨汤',1),
('驿达家庭三人预约套餐','米饭',3),
('驿达家庭三人预约套餐','百香果气泡饮',3),
('驿达轻食双人预约套餐','香煎三文鱼能量碗',1),
('驿达轻食双人预约套餐','虾仁藜麦沙拉',1),
('驿达轻食双人预约套餐','冰糖雪梨',2);

insert into setmeal_dish(setmeal_id,dish_id,name,price,copies)
select s.id,d.id,d.name,d.price,t.copies
from yida_stage4_setmeal_dish t
join setmeal s on s.name=t.setmeal_name
join dish d on d.name=t.dish_name
where not exists (
    select 1 from setmeal_dish sd where sd.setmeal_id=s.id and sd.dish_id=d.id
);
set @stage4_added_setmeal_links = row_count();

commit;

select @stage4_added_dishes as added_dishes,
       @stage4_added_flavors as added_flavors,
       @stage4_repaired_dish_images as repaired_dish_images,
       @stage4_added_setmeals as added_setmeals,
       @stage4_repaired_setmeal_images as repaired_setmeal_images,
       @stage4_added_setmeal_links as added_setmeal_links;

select c.id,c.name,count(d.id) as visible_dish_count
from category c
left join dish d on d.category_id=c.id and d.status=1
where c.type=1 and c.status=1
group by c.id,c.name
order by c.sort,c.id;

select c.id,c.name,count(s.id) as visible_setmeal_count
from category c
left join setmeal s on s.category_id=c.id and s.status=1
where c.type=2 and c.status=1
group by c.id,c.name
order by c.sort,c.id;

select count(*) as invalid_enabled_dish_images
from dish d join category c on c.id=d.category_id
where d.status=1 and c.status=1 and c.type=1
  and (d.image is null or trim(d.image)='' or d.image not like 'https://%');

select count(*) as invalid_enabled_setmeal_images
from setmeal s join category c on c.id=s.category_id
where s.status=1 and c.status=1 and c.type=2
  and (s.image is null or trim(s.image)='' or s.image not like 'https://%');

select name,count(*) as duplicate_count from dish group by name having count(*)>1;
select f.id as orphan_flavor_id from dish_flavor f left join dish d on d.id=f.dish_id where d.id is null;
select sd.id as orphan_setmeal_dish_id
from setmeal_dish sd
left join setmeal s on s.id=sd.setmeal_id
left join dish d on d.id=sd.dish_id
where s.id is null or d.id is null;

drop temporary table if exists yida_stage4_setmeal_dish;
drop temporary table if exists yida_stage4_setmeal;
drop temporary table if exists yida_stage4_dish;

