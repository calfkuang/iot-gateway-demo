package com.iot;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Modbus TCP采集器
 * 负责连接Modbus设备并读取寄存器数据
 */
public class ModbusCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(ModbusCollector.class);
    
    private final String host;
    private final int port;
    private final int slaveId;
    
    private ModbusMaster master;
    private boolean connected = false;
    
    public ModbusCollector(String host, int port, int slaveId) {
        this.host = host;
        this.port = port;
        this.slaveId = slaveId;
    }
    
    /**
     * 建立Modbus连接
     */
    public boolean connect() {
        try {
            TcpParameters params = new TcpParameters();
            params.setHost(InetAddress.getByName(host));
            params.setPort(port);
            params.setKeepAlive(true);
            
            master = ModbusMasterFactory.createModbusMasterTCP(params);
            Modbus.setAutoIncrementTransactionId(true);
            
            master.connect();
            connected = true;
            
            logger.info("Modbus连接成功: {}:{}, 从站ID: {}", host, port, slaveId);
            return true;
            
        } catch (UnknownHostException e) {
            logger.error("未知主机: {}", host);
            return false;
        } catch (ModbusIOException e) {
            logger.error("Modbus连接失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 读取保持寄存器(Holding Register)
     * @param address 寄存器地址
     * @return 寄存器值（转换为double）
     */
    public double readRegister(int address) {
        if (!connected || master == null) {
            throw new IllegalStateException("Modbus未连接");
        }
        
        try {
            // 读取1个保持寄存器
            int[] registers = master.readHoldingRegisters(slaveId, address, 1);
            
            if (registers != null && registers.length > 0) {
                // 将无符号整数转换为有符号值
                int value = registers[0];
                if (value > 32767) {
                    value = value - 65536;
                }
                return (double) value;
            }
            
            throw new RuntimeException("读取寄存器返回空数据");
            
        } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
            throw new RuntimeException("读取寄存器失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 读取多个寄存器
     * @param address 起始地址
     * @param quantity 数量
     * @return 寄存器值数组
     */
    public int[] readRegisters(int address, int quantity) {
        if (!connected || master == null) {
            throw new IllegalStateException("Modbus未连接");
        }
        
        try {
            return master.readHoldingRegisters(slaveId, address, quantity);
        } catch (ModbusProtocolException | ModbusNumberException | ModbusIOException e) {
            throw new RuntimeException("批量读取寄存器失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 断开Modbus连接
     */
    public void disconnect() {
        if (master != null && connected) {
            try {
                master.disconnect();
                logger.info("Modbus连接已关闭");
            } catch (ModbusIOException e) {
                logger.error("关闭Modbus连接失败: {}", e.getMessage());
            }
        }
        connected = false;
    }
    
    public boolean isConnected() {
        return connected;
    }
}
