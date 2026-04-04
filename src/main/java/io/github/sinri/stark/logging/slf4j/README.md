# SLF4j SPI 

基于SPI机制提供SLF4j的标准日志功能适配。

通过由 `io.github.sinri.stark.logging.base.LoggerFactory.universal()` 方法获取当前的 logger 工厂实例，
以此工厂实例所构造的 logger 实例作为 SLF4j 标准的 Logger 实例的底层能力承载。