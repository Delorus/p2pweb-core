package ru.sherb.nat;


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
import java.util.List;
import java.util.Timer;

public class NatPmpPortOpener implements IPortOpener, AutoCloseable {

    private static Logger log = LoggerFactory.getLogger(NatPmpPortOpener.class);

    private final Timer timer = new Timer("update mappedPort lifetime");
    private final Bus processBus;
    private final Bus networkBus;

    private PortMapper mapper;
    private MappedPort mappedPort;

    private final int port;

    public NatPmpPortOpener() {
        this(8021);
    }

    public NatPmpPortOpener(int port) {
        this.networkBus = NetworkGateway.create().getBus();
        this.processBus = ProcessGateway.create().getBus();
        this.port = port;
    }

    public int open() {

        try {
            List<PortMapper> mappers = PortMapperFactory.discover(networkBus, processBus);

            mappers.forEach(portMapper -> log.info("mapping mappedPort: " + portMapper.getSourceAddress()));

            if (mappers.isEmpty()) {
                log.warn("device with UPnP not discovered");
                return port;
            }

            mapper = mappers.get(0);

            this.mappedPort = mapper.mapPort(PortType.UDP, port, port, Duration.ofMinutes(5).getSeconds());

        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        timer.scheduleAtFixedRate(
                new LifetimePortUpdater(this),
                Duration.ofSeconds(getMappedPort().getLifetime()).toMillis(),
                Duration.ofSeconds(getMappedPort().getLifetime()).toMillis()
        );

        return getMappedPort().getInternalPort();
    }

    public void close() {
        timer.cancel();
        try {
            mapper.unmapPort(getMappedPort()); //TODO кидает экзепшен, что порт не был маплен через него

        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        networkBus.send(new KillNetworkRequest());
        processBus.send(new KillProcessRequest());
    }

    public synchronized MappedPort getMappedPort() {
        return mappedPort;
    }

    public synchronized void setMappedPort(MappedPort mappedPort) {
        this.mappedPort = mappedPort;
    }

    public PortMapper getMapper() {
        return mapper;
    }
}