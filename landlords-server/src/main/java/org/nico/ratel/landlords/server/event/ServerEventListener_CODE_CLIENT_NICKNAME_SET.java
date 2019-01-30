package org.nico.ratel.landlords.server.event;

import org.nico.noson.util.string.StringUtils;
import org.nico.ratel.landlords.channel.ChannelUtils;
import org.nico.ratel.landlords.entity.ClientSide;
import org.nico.ratel.landlords.enums.ClientEventCode;
import org.nico.ratel.landlords.helper.MapHelper;
import org.nico.ratel.landlords.print.SimplePrinter;
import org.nico.ratel.landlords.server.ServerContains;

public class ServerEventListener_CODE_CLIENT_NICKNAME_SET implements ServerEventListener{

	public static final int NICKNAME_MAX_LENGTH = 10;
	
	@Override
	public void call(ClientSide client, String nickname) {
		
		if (nickname.trim().length() > NICKNAME_MAX_LENGTH) { //判断设置的用户名是否超过10位
			String result = MapHelper.newInstance().put("invalidLength", nickname.trim().length()).json(); //向一个linkedHashMap中put设置的username的长度
			ChannelUtils.pushToClient(client.getChannel(), ClientEventCode.CODE_CLIENT_NICKNAME_SET, result);
		}else{
			// 判断nickname不能为空
			if (StringUtils.isBlank(nickname)){
				String result = MapHelper.newInstance().put("username is not blank", nickname).json();
				ChannelUtils.pushToClient(client.getChannel(), ClientEventCode.CODE_CLIENT_NICKNAME_SET, result, "username is not blank");
				SimplePrinter.serverLog(client.getId() + " | " + "username不能为空" + nickname);
				return;
			}

			// 判断username是否重复
			for (ClientSide value : ServerContains.CLIENT_SIDE_MAP.values()) {
				if (value.getNickname().equals(nickname)){
					String usernameNotTheSame = MapHelper.newInstance().put("username not the same", nickname).json();
					ChannelUtils.pushToClient(client.getChannel(), ClientEventCode.CODE_CLIENT_NICKNAME_SET, usernameNotTheSame, "username is not same");
					SimplePrinter.serverLog(client.getId() + " | " + nickname + " | " + "重复");
					return;
				}
			}

			ServerContains.CLIENT_SIDE_MAP.get(client.getId()).setNickname(nickname); //成功设置username
			ChannelUtils.pushToClient(client.getChannel(), ClientEventCode.CODE_SHOW_OPTIONS, null); //向客户端推送显示全局列表消息
		}
	}

}
