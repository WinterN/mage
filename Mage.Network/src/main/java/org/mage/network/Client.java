package org.mage.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mage.cards.decks.DeckCardLists;
import mage.constants.ManaType;
import mage.constants.PlayerAction;
import mage.game.match.MatchOptions;
import mage.game.tournament.TournamentOptions;
import mage.interfaces.ServerState;
import mage.players.net.UserSkipPrioritySteps;
import mage.utils.MageVersion;
import mage.view.DraftPickView;
import mage.view.MatchView;
import mage.view.RoomUsersView;
import mage.view.TableView;
import mage.view.TournamentView;
import mage.view.UserView;
import org.apache.log4j.Logger;
import org.mage.network.handlers.HeartbeatHandler;
import org.mage.network.handlers.PingMessageHandler;
import org.mage.network.handlers.client.ChatMessageHandler;
import org.mage.network.handlers.client.ChatRoomHandler;
import org.mage.network.handlers.client.ClientRegisteredMessageHandler;
import org.mage.network.handlers.client.InformClientMessageHandler;
import org.mage.network.handlers.client.ServerMessageHandler;
import org.mage.network.interfaces.MageClient;
import org.mage.network.model.MessageType;
import org.mage.network.model.RegisterClientMessage;

/**
 *
 * @author BetaSteward
 */
public class Client {
    
    private static final Logger logger = Logger.getLogger(Client.class);
    
    private static final int IDLE_PING_TIME = 30;
    private static final int IDLE_TIMEOUT = 60;

    private final MageClient client;
//    private final MessageHandler h;
    private final ChatRoomHandler chatRoomHandler;
    private final ChatMessageHandler chatMessageHandler;
    private final InformClientMessageHandler informClientMessageHandler;
    private final ClientRegisteredMessageHandler clientRegisteredMessageHandler;
    private final ServerMessageHandler serverMessageHandler;
    
    private Channel channel;
    private EventLoopGroup group;
    private String username;    
    
    public Client(MageClient client) {
        this.client = client;
//        h = new MessageHandler();
        chatRoomHandler = new ChatRoomHandler();
        chatMessageHandler = new ChatMessageHandler(client);
        informClientMessageHandler = new InformClientMessageHandler(client);
        clientRegisteredMessageHandler = new ClientRegisteredMessageHandler(client);
        serverMessageHandler = new ServerMessageHandler();
    }
    
    public boolean connect(String userName, String host, int port, MageVersion version) {
        
        this.username = userName;

        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ClientInitializer());
            
            clientRegisteredMessageHandler.setUserName(userName);
            clientRegisteredMessageHandler.setVersion(version);
            channel = b.connect(host, port).sync().channel();
            clientRegisteredMessageHandler.registerClient();
            client.connected(userName + "@" + host + ":" + port + " ");
            return true;
        } catch (InterruptedException ex) {
            logger.fatal("Error connecting", ex);
            client.inform("Error connecting", MessageType.ERROR);
            group.shutdownGracefully();
        }
        return false;
    }
    
    private class ClientInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            
            ch.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
            ch.pipeline().addLast(new ObjectEncoder());

            ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(IDLE_TIMEOUT, IDLE_PING_TIME, 0));
            ch.pipeline().addLast("heartbeatHandler", new HeartbeatHandler());
            ch.pipeline().addLast("pingMessageHandler", new PingMessageHandler());

//            ch.pipeline().addLast("h", h);
            ch.pipeline().addLast("chatMessageHandler", chatMessageHandler);
            ch.pipeline().addLast("informClientMessageHandler", informClientMessageHandler);
            ch.pipeline().addLast("clientRegisteredMessageHandler", clientRegisteredMessageHandler);
            ch.pipeline().addLast("chatRoomHandler", chatRoomHandler);
            ch.pipeline().addLast("serverMessageHandler", serverMessageHandler);

        }
    }
    
    public void disconnect() {
        
        try {
            channel.closeFuture().sync();
        } catch (InterruptedException ex) {
            logger.fatal("Error disconnecting", ex);
        } finally {
            group.shutdownGracefully();
        }
    }
    
    public boolean isConnected() {
        if (channel != null)
            return channel.isActive();
        return false;
    }
    
    public void sendChatMessage(UUID chatId, String message) {
        chatMessageHandler.sendMessage(chatId, message);
    }

    public void joinChat(UUID chatId) {
        chatRoomHandler.joinChat(chatId);
    }
    
    public void leaveChat(UUID chatId) {
        chatRoomHandler.leaveChat(chatId);
    }

    public void sendPlayerUUID(UUID gameId, UUID id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void sendPlayerBoolean(UUID gameId, boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getUserName() {
        return username;
    }

    public ServerState getServerState() {
        return client.getServerState();
    }

    public boolean submitDeck(UUID tableId, DeckCardLists deckCardLists) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void updateDeck(UUID tableId, DeckCardLists deckCardLists) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean sendFeedback(String title, String type, String message, String email) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean joinTournamentTable(UUID roomId, UUID tableId, String playerName, String human, int i, DeckCardLists importDeck, String text) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean joinTable(UUID roomId, UUID tableId, String playerName, String human, int i, DeckCardLists importDeck, String text) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public TableView createTable(UUID roomId, MatchOptions options) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void removeTable(UUID roomId, UUID tableId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getSessionId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public TableView createTournamentTable(UUID roomId, TournamentOptions tOptions) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void updatePreferencesForServer(int selectedAvatar, boolean selected, boolean selected0, UserSkipPrioritySteps userSkipPrioritySteps) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean isTableOwner(UUID roomId, UUID tableId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public UUID getTableChatId(UUID tableId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean startMatch(UUID roomId, UUID tableId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean startTournament(UUID roomId, UUID tableId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean leaveTable(UUID roomId, UUID tableId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void swapSeats(UUID roomId, UUID tableId, int row, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void sendPlayerAction(PlayerAction playerAction, UUID gameId, UUID relatedUserId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public TableView getTable(UUID roomId, UUID tableId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void watchTournamentTable(UUID tableId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public UUID getTournamentChatId(UUID tournamentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean joinTournament(UUID tournamentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void quitTournament(UUID tournamentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public TournamentView getTournament(UUID tournamentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public UUID getMainRoomId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void watchTable(UUID roomId, UUID tableId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void replayGame(UUID gameId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public UUID getRoomChatId(UUID roomId) {
        try {
            return chatRoomHandler.getChatRoomId(roomId);
        } catch (Exception ex) {
            logger.error("Error getting chat room id", ex);
        }
        return null;
    }

    public List<String> getServerMessages() {
        try {
            return serverMessageHandler.getServerMessages();
        } catch (Exception ex) {
            logger.error("Error getting server messages", ex);
        }
        return null;
    }

    public Collection<TableView> getTables(UUID roomId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Collection<MatchView> getFinishedMatches(UUID roomId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public Collection<RoomUsersView> getRoomUsers(UUID roomId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void sendPlayerInteger(UUID gameId, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void sendPlayerString(UUID gameId, String special) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void sendPlayerManaType(UUID gameId, UUID playerId, ManaType manaType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void cheat(UUID gameId, UUID playerId, DeckCardLists importDeck) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public UUID getGameChatId(UUID gameId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean joinGame(UUID gameId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean watchGame(UUID gameId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean startReplay(UUID gameId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void stopWatching(UUID gameId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void stopReplay(UUID gameId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void nextPlay(UUID gameId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void previousPlay(UUID gameId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void skipForward(UUID gameId, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void quitMatch(UUID gameId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean joinDraft(UUID draftId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public DraftPickView sendCardPick(UUID draftId, UUID id, Set<UUID> cardsHidden) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void sendCardMark(UUID draftId, UUID id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void quitDraft(UUID draftId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void sendBroadcastMessage(String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void disconnectUser(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void removeTable(UUID uuid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void endUserSession(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public List<UserView> getUsers() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}