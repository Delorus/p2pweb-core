package ru.sherb;


import com.offbynull.portmapper.PortMapperFactory;
import com.offbynull.portmapper.gateway.Bus;
import com.offbynull.portmapper.gateways.network.NetworkGateway;
import com.offbynull.portmapper.gateways.network.internalmessages.KillNetworkRequest;
import com.offbynull.portmapper.gateways.process.ProcessGateway;
import com.offbynull.portmapper.gateways.process.internalmessages.KillProcessRequest;
import com.offbynull.portmapper.mapper.MappedPort;
import com.offbynull.portmapper.mapper.PortMapper;
import com.offbynull.portmapper.mapper.PortType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Timer;

public class NatPmpPortOpener {

    private static Logger log = LoggerFactory.getLogger(NatPmpPortOpener.class);

    private final Timer timer = new Timer("update port lifetime");
    private final Bus processBus;
    private final Bus networkBus;

    private PortMapper mapper;
    private MappedPort port;

    public NatPmpPortOpener() {
        this.networkBus = NetworkGateway.create().getBus();
        this.processBus = ProcessGateway.create().getBus();
    }

    public void open(int port) throws InterruptedException {

        //TODO смотреть шлюз по умолчанию
        List<PortMapper> mappers = PortMapperFactory.discover(networkBus, processBus);
        mappers.forEach(portMapper -> log.info("mapping port: " + portMapper.getSourceAddress()));

        if (mappers.isEmpty()) {
            log.warn("port already opened or device with UPnP not discovered");
            return;
        }

        mapper = mappers.get(0);

        this.port = mapper.mapPort(PortType.UDP, port, port, Duration.ofMinutes(5).getSeconds());

        timer.scheduleAtFixedRate(
                new LifetimePortUpdater(this),
                Duration.ofSeconds(getPort().getLifetime()).toMillis(),
                Duration.ofSeconds(getPort().getLifetime()).toMillis()
        );

    }

    public void close() {
        timer.cancel();
        try {
            mapper.unmapPort(getPort()); //TODO кидает экзепшен, что порт не был маплен через него

        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        networkBus.send(new KillNetworkRequest());
        processBus.send(new KillProcessRequest());
    }

    public synchronized MappedPort getPort() {
        return port;
    }

    public synchronized void setPort(MappedPort port) {
        this.port = port;
    }

    public PortMapper getMapper() {
        return mapper;
    }
}