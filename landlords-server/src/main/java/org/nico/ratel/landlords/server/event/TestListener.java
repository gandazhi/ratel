package org.nico.ratel.landlords.server.event;

import io.netty.channel.Channel;
import org.nico.ratel.landlords.entity.ClientSide;

/**
 * @Auther: gandazhi
 * @Date: 2019-01-16 22:06
 */
public class TestListener implements ServerEventListener {

    @Override
    public void call(ClientSide client, String data) {
        Channel channel = client.getChannel();

    }
}
