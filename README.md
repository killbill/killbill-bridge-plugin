# Overview


The `killbill-bridge-plugin`, or in short `bridge`, is intended to bridge two deployments of Kill Bill, one used as a subscription/invoice engine (`KB Subscription` or in short `KB-S`) , and the other one used as an internal payment gateway (`KB Payment` or in short `KB-P`):


![alt text](https://github.com/killbill/killbill-bridge-plugin/blob/master/assets/KillBillBridgePlugin.png "Bridge Deployment")

The `bridge` really plays two different functions, one of which is to offer a payment control layer, by implementing the [Kill Bill payment control plugin api](https://github.com/killbill/killbill-plugin-api/blob/master/control/src/main/java/org/killbill/billing/control/plugin/api/PaymentControlPluginApi.java), and the other is the payment plugin itself by implementing the [Kill Bill payment plugin api](https://github.com/killbill/killbill-plugin-api/blob/master/payment/src/main/java/org/killbill/billing/payment/plugin/api/PaymentPluginApi.java). The  payment control layer is used to modify incoming payment requests prior it reaches the payment plugin operation itself (e.g `authorization`).

The `KB-S` system will provide all the normal functionality, including payment operations, but those will be delegated to `KB-P`. There are numerous reasons why a company would want to adopt this model:

* Keeping invoice/subscription engine separate from payment system fits well in a micro-services architecture model
* Related to previous micro-services architecture point, this also allows to decouple the responsabilties -- different teams, deployment schedule,...
* Allow to fully leverage the internal payment gateway to implement more advanced things like payment routing, payment optimization, ... while keeping all this aspect hidden from core subscription/invoice engine
* Different compliance scopes (SOX, PII, PCI, ...)


## Models

There are two main models that we can identify:

1. The `KB-P` is used as a simple internal payment gateway but does not do any dynamic payment routing: In this model, each 
`paymentMethod` associated with an `Account` in `KB-S` reflects the plugin that should be used (e.g `stripe`) on the `KB-P` side -- that is, the `pluginName` associated to each `paymentMethod` will correctly show `killbill-stripe` in this example.  The `KB-P` is completely transparent and really works as a proxy.

2. In this second model, the `KB-P` is used as a dynamic payment gateway. It can route payments based on rules such as latency, errors, BIN-level optimization, business-level rules -- mimimum volume to meet contract terms, ... In this case, creating a `paymentMethod` associated with an `Account` in the `KB-S` will **not** create a matching  `paymentMethod` on the `KB-P` side; instead, the `bridge` will initiate all the payment requests with a null `paymentMethodId` and rely on the control payment layer on the `KB-P` side to automatically do the payment routing.


# Configuration


This plugin implements the [Kill Bill payment plugin api](https://github.com/killbill/killbill-plugin-api/blob/master/payment/src/main/java/org/killbill/billing/payment/plugin/api/PaymentPluginApi.java) and is intended to connect as a bridge between a Kill Bill system operating for handling billing components (Subscriptions, Invoices, ...) 
and another Kill Bill system operating for handling payments.

The plugin will need to have the default configuration parameter to connect to the remote Kill Bill (payment) system.
In addition for each tenant, the details of the `api_key` and `api_secret` will be required.


* `org.killbill.billing.plugin.bridge.serverHost`
* `org.killbill.billing.plugin.bridge.serverPort`
* `org.killbill.billing.plugin.bridge.username`
* `org.killbill.billing.plugin.bridge.password`
* `org.killbill.billing.plugin.bridge.apiKey`
* `org.killbill.billing.plugin.bridge.apiSecret`
* `org.killbill.billing.plugin.bridge.proxyHost` (Optional)
* `org.killbill.billing.plugin.bridge.proxyPort` (Optional)
* `org.killbill.billing.plugin.bridge.connectTimeOut` (Optional)
* `org.killbill.billing.plugin.bridge.readTimeOut` (Optional)
* `org.killbill.billing.plugin.bridge.requestTimeout` (Optional)
* `org.killbill.billing.plugin.bridge.strictSSL` (Optional)
* `org.killbill.billing.plugin.bridge.SSLProtocol` (Optional)

# Internals



