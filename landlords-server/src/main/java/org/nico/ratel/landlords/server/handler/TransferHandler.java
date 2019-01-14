package org.nico.ratel.landlords.server.handler;

import org.nico.ratel.landlords.channel.ChannelUtils;
import org.nico.ratel.landlords.entity.ClientSide;
import org.nico.ratel.landlords.entity.ServerTransferData.ServerTransferDataProtoc;
import org.nico.ratel.landlords.enums.ClientEventCode;
import org.nico.ratel.landlords.enums.ClientRole;
import org.nico.ratel.landlords.enums.ClientStatus;
import org.nico.ratel.landlords.enums.ServerEventCode;
import org.nico.ratel.landlords.print.SimplePrinter;
import org.nico.ratel.landlords.server.ServerContains;
import org.nico.ratel.landlords.server.event.ServerEventListener;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class TransferHandler extends ChannelInboundHandlerAdapter{

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		//客户端与服务端的连接注册
		Channel ch = ctx.channel();
		
		//init client info
		ClientSide clientSide = new ClientSide(getId(ctx.channel()), ClientStatus.TO_CHOOSE, ch);
		clientSide.setNickname(String.valueOf(clientSide.getId()));
		clientSide.setRole(ClientRole.PLAYER); //设置客户端权限为player
		
		ServerContains.CLIENT_SIDE_MAP.put(clientSide.getId(), clientSide);
		SimplePrinter.serverLog("Has client connect to the server：" + clientSide.getId());
		
		ChannelUtils.pushToClient(ch, ClientEventCode.CODE_CLIENT_CONNECT, String.valueOf(clientSide.getId())); //把客户端连接到服务端的事情推送给 客户端
		ChannelUtils.pushToClient(ch, ClientEventCode.CODE_CLIENT_NICKNAME_SET, null); //向客户端推送设置用户名
	}

	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof ServerTransferDataProtoc) {
			ServerTransferDataProtoc serverTransferData = (ServerTransferDataProtoc) msg;
			ServerEventCode code = ServerEventCode.valueOf(serverTransferData.getCode());
			if(code != null && code != ServerEventCode.CODE_CLIENT_HEAD_BEAT) {
				ClientSide client = ServerContains.CLIENT_SIDE_MAP.get(getId(ctx.channel())); // 根据channel从客户端的map中获取client
				SimplePrinter.serverLog(client.getId() + " | " + client.getNickname() + " do:" + code.getMsg());
				ServerEventListener.get(code).call(client, serverTransferData.getData());
			}
		}
	}

	//捕获异常
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(cause instanceof java.io.IOException) {
			clientOfflineEvent(ctx.channel());
		}else {
			SimplePrinter.serverLog("ERROR：" + cause.getMessage());
			cause.printStackTrace();
		}
	}

	// 用户事件
    @Override  
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		//心跳包
        if (evt instanceof IdleStateEvent) {  
            IdleStateEvent event = (IdleStateEvent) evt;  
            if (event.state() == IdleState.READER_IDLE) {  
                try{
                	clientOfflineEvent(ctx.channel());
                	ctx.channel().close();
                }catch(Exception e){
                }
            }  
        } else {  
            super.userEventTriggered(ctx, evt);  
        }  
    }  
	
    private int getId(Channel channel){
    	String longId = channel.id().asLongText();
    	Integer clientId = ServerContains.CHANNEL_ID_MAP.get(longId);
    	if(null == clientId){
    		//空 说明该客户端与服务端还没有连接，则把id放入map中
    		clientId = ServerContains.getClientId();
    		ServerContains.CHANNEL_ID_MAP.put(longId, clientId);
    	}
    	return clientId;
    }
    
    private void clientOfflineEvent(Channel channel){
    	int clientId = getId(channel);
    	ClientSide client = ServerContains.CLIENT_SIDE_MAP.get(clientId);
    	if(client != null) {
			SimplePrinter.serverLog("Has client exit to the server：" + clientId + " | " + client.getNickname());
			ServerEventListener.get(ServerEventCode.CODE_CLIENT_OFFLINE).call(client, null);
		}
    }

    //客户端与服务端断开连接,断开连接的时候手动释放连接
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		clientOfflineEvent(channel);
	}
}
