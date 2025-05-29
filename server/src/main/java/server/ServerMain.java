package server;

import common.util.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.DatabaseConfig;
import server.handler.HttpServerHandler;

import java.util.concurrent.TimeUnit;

/**
 * Главный класс сервера системы лояльности
 */
public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
    private static final int MAX_CONTENT_LENGTH = 1024 * 1024; // 1MB
    private static final int READ_TIMEOUT_SECONDS = 60;
    
    private final int port;
    
    public ServerMain(int port) {
        this.port = port;
    }
    
    /**
     * Запускает сервер
     * @throws Exception если произошла ошибка при запуске сервера
     */
    public void start() throws Exception {
        // Инициализируем пул соединений с базой данных
        logger.info("Инициализация базы данных...");
        DatabaseConfig.getDataSource(); // Инициализация пула соединений
        
        // Настройка групп потоков для Netty
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            logger.info("Запуск HTTP сервера на порту {}...", port);
            
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new ReadTimeoutHandler(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                                new HttpServerCodec(),
                                new HttpObjectAggregator(MAX_CONTENT_LENGTH),
                                new HttpServerHandler()
                        );
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // Запуск сервера
            ChannelFuture f = b.bind(port).sync();
            logger.info("Сервер запущен. Порт: {}", port);
            
            // Ждем завершения работы сервера
            f.channel().closeFuture().sync();
        } finally {
            // Корректное завершение работы
            logger.info("Завершение работы сервера...");
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            
            // Закрываем соединения с базой данных
            DatabaseConfig.closeDataSource();
            logger.info("Сервер остановлен");
        }
    }
    
    /**
     * Точка входа в приложение
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        try {
            int port = Constants.SERVER_PORT;
            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    logger.warn("Неверный формат порта: {}. Используется порт по умолчанию: {}", 
                            args[0], Constants.SERVER_PORT);
                }
            }
            
            new ServerMain(port).start();
        } catch (Exception e) {
            logger.error("Ошибка при запуске сервера", e);
            System.exit(1);
        }
    }
}
