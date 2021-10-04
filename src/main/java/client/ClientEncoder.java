package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Класс с занятия
 */
public class ClientEncoder extends MessageToMessageEncoder<String> {

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) {
        out.add(msg.getBytes(StandardCharsets.UTF_8));
    }
}
