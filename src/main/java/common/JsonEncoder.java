package common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class JsonEncoder extends MessageToMessageEncoder<Request> {
    ObjectMapper om = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, Request msg, List<Object> out) throws Exception {
        byte[] bytes = om.writeValueAsBytes(msg);
        out.add(bytes);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}
