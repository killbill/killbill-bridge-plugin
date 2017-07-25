Kill Bill bridge plugin
=======================

This plugin implements the [Kill Bill payment plugin api](https://github.com/killbill/killbill-plugin-api/blob/master/payment/src/main/java/org/killbill/billing/payment/plugin/api/PaymentPluginApi.java) and is intended to connect as a bridge between a Kill Bill system operating for handling billing components (Subscriptions, Invoices, ...) 
and another Kill Bill system operating for handling payments.


= Configuration =

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

