/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2018 jPOS Software SRL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpos.qrest;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.jpos.transaction.Context;

import static io.netty.buffer.Unpooled.copiedBuffer;

public class RestSession extends ChannelInboundHandlerAdapter {
    private RestServer server;

    RestSession(RestServer server) {
        this.server = server;
    }

    @Override
    public void channelRead(ChannelHandlerContext ch, Object msg) throws Exception {
        Context ctx = new Context();
        if (msg instanceof FullHttpRequest) {
            final FullHttpRequest request = (FullHttpRequest) msg;
            ctx.put(Constants.SESSION, ch);
            ctx.put(Constants.REQUEST, request);
            server.queue(ctx);
        } else {
            super.channelRead(ch, msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        server.getLog().warn(cause);
        ctx.writeAndFlush(new DefaultFullHttpResponse(
          HttpVersion.HTTP_1_1,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          copiedBuffer(cause.getMessage().getBytes())
        ));
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        server.getLog().info("accepted: " + ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        server.getLog().info("closed: " + ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState e = ((IdleStateEvent) evt).state();
            if (e == IdleState.READER_IDLE) {
                server.getLog().info("timeout " + ctx.channel());
                ctx.fireChannelInactive();
                ctx.close();
            }
        }
    }
}
