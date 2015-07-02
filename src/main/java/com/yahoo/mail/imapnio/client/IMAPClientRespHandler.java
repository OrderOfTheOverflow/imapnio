/**
 *
 */
package com.yahoo.mail.imapnio.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * @author kraman
 *
 */
public class IMAPClientRespHandler extends MessageToMessageDecoder<IMAPResponse> {

    private IMAPSession session;

    /** logger. */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPClientRespHandler.class);

    /**
     * Creates a IMAPClient response handler. The last of the handlers in the chain before giving a callback to the client.
     *
     * @param session
     *            - IMAP session
     */
    public IMAPClientRespHandler(IMAPSession session) {
        this.session = session;
    }

    /**
     * Decode the incoming IMAP command/response from server. Call the client listener when done.
     *
     * @param ctx
     *            - channel context
     * @param msg
     *            - incoming IMAP message
     * @param out
     *            - message to be sent to the next handler in the chain
     */
    @Override
    public void decode(ChannelHandlerContext ctx, IMAPResponse msg, List<Object> out) throws Exception {
        log.info("< " + msg + " state:" + session.getState());
        if (session.getState() == IMAPSessionState.IDLE_REQUEST) {
        	if (msg.readAtomString().equals("idling")) {
                session.setState(IMAPSessionState.IDLING);
            }
        	session.addResponse(msg);
        }
        if (session.getState() == IMAPSessionState.IDLING) {
        	session.getClientListener(session.getIdleTag()).onResponse (session, session.getIdleTag(), Arrays.asList(msg));
		} else {
			if (null != msg.getTag()) {
				IMAPClientListener listener = session.removeClientListener(msg
						.getTag());
				if (null != listener) {
					List<IMAPResponse> responses = new ArrayList<IMAPResponse>(
							session.getResponseList());
					session.resetResponseList();
					listener.onResponse(session, msg.getTag(),
							responses);
				}
			} else {
				session.addResponse(msg);
				// Pass along message without modifying it.
				out.add(msg);
			}
		}
    }

}
