# 支付服务示例

这是一个集成微信支付（JSAPI/APP/Native/H5）和支付宝支付（JSAPI/APP/PAGE/WAP）的支付服务示例。

## 术语说明

为了方便对支付服务示例的理解，下面将使用一些术语来进行规定。

1. 支付方式：支付方式就是微信、支付宝所提供的支付类型，比如：微信小程序支付、微信APP支付、支付宝小程序支付等。
2. 入口：入口就是表示支付是从什么地方发起的，假设我们的项目涵盖了微信小程序A，微信小程序B，支付宝小程序A，支付宝小程序B，APP1，APP2等，而其中微信小程序AB使用同一种支付方式（都是微信小程序支付），所以采用入口来进行区分。
3. 业务方：业务方就是需要使用支付服务的业务，比如：下单商品、购买会员、充值卡卷、充值钱包等。
4. 交易：交易就是在支付服务中支付和退款动作的统称，每发生一次支付或退款就是一次交易，每次交易会产生一个唯一的交易编号。

## 如何运行支付服务？

支付服务示例使用到的框架和中间件

- Spring Cloud Alibaba
- MyBatis Plus
- Nacos
- MySQL
- Redis
- Kafka

由于使用了Nacos，故在 `resources` 中将只配置基础配置，其他所需要的配置都将放置在Nacos中。
需要在Nacos中添加 `pay.yaml` 和 `pay-ext.yaml` 两个配置文件，其中前者用来配置框架配置，如MySQL、Kafka这些。 后者用来配置自定义参数。

运行服务时需要系统参数：`env=环境;nacos.address=Nacos地址` 。

## 基本用法说明

#### 1. 如何发起支付？

支付服务上提供预支付接口，预支付接口将返回交易ID和预支付信息，交易ID由业务方进行关联保存，预支付信息则返回给前端拉起支付弹窗。

> 详细说明可查阅 `com.lx.pay.controller.BizController.prepay()` 。

#### 2. 如何取消支付？

支付服务上提供关闭支付接口，业务方可在支付完成之前，主动将支付关闭。

> 详细说明可查阅 `com.lx.pay.controller.BizController.closePay()` 。

#### 3. 如何查询支付是否成功？

支付服务上提供查询支付接口，业务方可主动查询当前支付的结果。

> 详细说明可查阅 `com.lx.pay.controller.BizController.queryPay()` 。

#### 4. 除了主动查询，能否被动将支付结果通知给业务方？

在预支付接口中，需要传入 `bizMqTopic` ，支付服务会将支付结果发送给 `bizMqTopic` 的消息队列中。业务方需要编写消费者来监听支付结果的通知。

#### 5. 如何进行退款？

支付服务上提供退款接口，业务方可在支付完成之后发起退款。退款仅针对某次支付交易，即只能退某次支付交易上的总金额或部分金额。

> 详细说明可查阅 `com.lx.pay.controller.BizController.refund()` 。

## 配置所需要使用的支付方式

#### 入口配置

```yaml
pay:
  ins:
    in1: wechatApp
    in2: wechatJSAPI
    in3: alipayApp
```

入口需要是全局唯一的。

#### 微信支付配置

> 不支持服务商代研模式，仅支持商家自研模式。
> 使用v3的API实现。

每个公司可能会有多个微信支付商户，所以支付服务需要支持多商户的微信支付方式。那么要实现多商户，则需要进行如下配置：

```yaml
pay:
  wechat:
    ins:
      in1: appId1
      in2: appId2
      in3: appId3
    merchants:
      - merchant-id: ''
        private-key-path: ''
        serial-number: ''
        api-v3-key: ''
        appIds:
          - appId1
          - appId2
      - merchant-id: ''
        private-key-path: ''
        serial-number: ''
        api-v3-key: ''
        appIds:
          - appId3
```

- 配置入口与微信appId映射，两者处于一对一关系。
- 每个商户都需要配置商户ID、私钥路径、证书序号、API v3 Key、商户绑定的appId列表。

这样通过入口就可以获取到对应的appId，根据appId就可以找到对应的商户配置。

支持的支付方式实现类Bean名：

- wechatApp
- wechatH5
- wechatJSAPI
- wechatNative

#### 支付宝支付

> 不支持服务商代研模式，仅支持商家自研模式。

与微信一样，支持多支付宝帐户实现。

```yaml
pay:
  alipay:
    ins:
      in1: appId1
      in2: appId2
      in3: appId3
    server-url: ''
    accounts:
      - public-key: ''
        apps:
          appId1: private-key1
          appId2: private-key2
      - public-key: ''
        apps:
          appId2: private-key3
```

- 配置入口与支付宝appId映射，两者处于一对一关系。
- 配置支付宝网关地址。
- 每个帐号都需要配置支付宝公钥、appId与应用私钥的映射。

支持的支付方式实现类Bean名：

- alipayApp
- alipayWap
- alipayJSAPI
- alipayPage

## 关于通知说明

todo