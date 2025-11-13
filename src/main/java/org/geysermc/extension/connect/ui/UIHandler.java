/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GeyserConnect
 */

package org.geysermc.extension.connect.ui;

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.geysermc.extension.connect.GeyserConnect;
import org.geysermc.extension.connect.language.LanguageManager;
import org.geysermc.extension.connect.utils.Server;
import org.geysermc.extension.connect.utils.ServerCategory;
import org.geysermc.extension.connect.utils.ServerManager;
import org.geysermc.extension.connect.utils.Utils;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.session.GeyserSession;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UIHandler {
    private final GeyserSession session;
    private final BedrockPacketHandler originalPacketHandler;

    public UIHandler(GeyserSession session, BedrockPacketHandler originalPacketHandler) {
        this.session = session;
        this.originalPacketHandler = originalPacketHandler;
    }

    public void initialiseSession() {
        String message = "";
        try {
            File messageFile = Utils.fileOrCopiedFromResource(GeyserConnect.instance().config().welcomeFile(), "welcome.txt");
            message = new String(Utils.readAllBytes(messageFile), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }

        if (!message.trim().isEmpty()) {
            session.sendForm(CustomForm.builder()
                .title("Notice")
                .label(message)
                .resultHandler((customForm, customFormResponseFormResponseResult) -> {
                    sendMainMenu();
                })
                .build());
        } else {
            sendMainMenu();
        }
    }

    public void sendMainMenu() {
        LanguageManager lang = GeyserConnect.instance().languageManager();
        
        SimpleForm.Builder mainMenu = SimpleForm.builder()
            .title(lang.get("ui.title.main_menu"))
            .button(lang.get("ui.button.official_servers"))
            .button(lang.get("ui.button.geyser_servers"));

        // Add a buttons for custom servers
        if (GeyserConnect.instance().config().customServers().enabled()) {
            mainMenu.button(lang.get("ui.button.custom_servers"));
            mainMenu.button(lang.get("ui.button.direct_connect"));
        }

        mainMenu
            .button(lang.get("ui.button.disconnect"))
            .closedResultHandler(response -> {
                sendMainMenu();
            })
            .invalidResultHandler(response -> {
                session.disconnect("disconnectionScreen.disconnected");
            })
            .validResultHandler(response -> {
                switch (response.clickedButtonId()) {
                    case 0:
                        sendServersMenu(ServerCategory.OFFICIAL);
                        return;
                    case 1:
                        sendServersMenu(ServerCategory.GEYSER);
                        return;
                    default:
                        if (GeyserConnect.instance().config().customServers().enabled()) {
                            switch (response.clickedButtonId()) {
                                case 2:
                                    sendServersMenu(ServerCategory.CUSTOM);
                                    return;
                                case 3:
                                    sendDirectConnectMenu();
                                    return;
                            }
                        }
                        break;
                }

                session.disconnect("disconnectionScreen.disconnected");
            });

        session.sendForm(mainMenu);
    }

    public void sendServersMenu(ServerCategory category) {
        LanguageManager lang = GeyserConnect.instance().languageManager();
        
        SimpleForm.Builder serversMenu = SimpleForm.builder()
            .title(lang.getMessage("ui.title.servers", category.title() + " Servers"));

        List<Server> servers;
        if (category == ServerCategory.CUSTOM) {
            servers = ServerManager.getServers(session);
        } else {
            servers = Utils.getServers(category);
        }

        for (Server server : servers) {
            serversMenu.button(server.title(), server.formImage());
        }

        if (category == ServerCategory.CUSTOM) {
            serversMenu.button(lang.get("ui.button.edit_servers"));
        }

        serversMenu
            .button(lang.get("ui.button.back"))
            .closedOrInvalidResultHandler(response -> {
                sendMainMenu();
            })
            .validResultHandler(response -> {
                if (category == ServerCategory.CUSTOM) {
                    if (response.clickedButtonId() == servers.size()) {
                        sendEditServersMenu();
                        return;
                    } else if (response.clickedButtonId() == servers.size() + 1) {
                        sendMainMenu();
                        return;
                    }
                } else if (response.clickedButtonId() == servers.size()) {
                    sendMainMenu();
                    return;
                }

                Server server = servers.get(response.clickedButtonId());
                Utils.sendToServer(session, originalPacketHandler, server);
            });

        session.sendForm(serversMenu);
    }

    public void sendEditServersMenu() {
        LanguageManager lang = GeyserConnect.instance().languageManager();
        
        SimpleForm.Builder editServersMenu = SimpleForm.builder()
            .title(lang.get("ui.title.edit_servers"))
            .content(lang.get("ui.content.edit_servers"));

        List<Server> servers = ServerManager.getServers(session);

        for (Server server : servers) {
            editServersMenu.button(server.title(), server.formImage());
        }

        editServersMenu
            .button(lang.get("ui.button.add_server"))
            .button(lang.get("ui.button.back"))
            .closedOrInvalidResultHandler(response -> {
                sendServersMenu(ServerCategory.CUSTOM);
            })
            .validResultHandler(response -> {
                if (response.clickedButtonId() == servers.size()) {
                    sendAddServerMenu();
                    return;
                } else if (response.clickedButtonId() == servers.size() + 1) {
                    sendServersMenu(ServerCategory.CUSTOM);
                    return;
                }

                Server server = servers.get(response.clickedButtonId());
                sendServerOptionsMenu(server);
            });

        session.sendForm(editServersMenu);
    }

    public void sendAddServerMenu() {
        LanguageManager lang = GeyserConnect.instance().languageManager();
        
        session.sendForm(CustomForm.builder()
            .title(lang.get("ui.title.add_server"))
            .input(lang.get("ui.label.ip"), "play.cubecraft.net")
            .input(lang.get("ui.label.port"), "25565", "25565")
            .toggle(lang.get("ui.label.online_mode"), true)
            .toggle(lang.get("ui.label.bedrock_server"), false)
            .closedOrInvalidResultHandler(response -> {
                sendEditServersMenu();
            })
            .validResultHandler(response -> {
                String ip = response.asInput(0);
                int port = Integer.parseInt(response.asInput(1));
                boolean onlineMode = response.asToggle(2);
                boolean geyserServer = response.asToggle(3);

                Server server = new Server(ip, port, onlineMode, geyserServer, null, null, ServerCategory.CUSTOM);
                ServerManager.addServer(session, server);
                sendEditServersMenu();
            }));
    }

    public void sendServerOptionsMenu(Server server) {
        LanguageManager lang = GeyserConnect.instance().languageManager();
        
        session.sendForm(SimpleForm.builder()
            .title(lang.get("ui.title.server_options"))
            .content(server.title())
            .button(lang.get("ui.button.edit_server"))
            .button(lang.get("ui.button.delete_server"))
            .button(lang.get("ui.button.back"))
            .closedOrInvalidResultHandler(response -> {
                sendEditServersMenu();
            })
            .validResultHandler(response -> {
                switch (response.clickedButtonId()) {
                    case 0:
                        sendEditServerMenu(server);
                        return;
                    case 1:
                        sendDeleteServerMenu(server);
                        return;
                    case 2:
                        sendEditServersMenu();
                        return;
                }
            }));
    }

    public void sendEditServerMenu(Server server) {
        LanguageManager lang = GeyserConnect.instance().languageManager();
        
        int serverIndex = ServerManager.getServerIndex(session, server);
        session.sendForm(CustomForm.builder()
            .title(lang.get("ui.title.edit_server"))
            .input(lang.get("ui.label.ip"), server.address(), server.address())
            .input(lang.get("ui.label.port"), String.valueOf(server.port()), String.valueOf(server.port()))
            .toggle(lang.get("ui.label.online_mode"), server.online())
            .toggle(lang.get("ui.label.bedrock_server"), server.bedrock())
            .closedOrInvalidResultHandler(response -> {
                sendServerOptionsMenu(server);
            })
            .validResultHandler(response -> {
                String ip = response.asInput(0);
                int port = Integer.parseInt(response.asInput(1));
                boolean onlineMode = response.asToggle(2);
                boolean geyserServer = response.asToggle(3);

                Server newServer = new Server(ip, port, onlineMode, geyserServer, null, null, ServerCategory.CUSTOM);
                ServerManager.updateServer(session, serverIndex, newServer);
                sendServerOptionsMenu(newServer);
            }));
    }

    public void sendDeleteServerMenu(Server server) {
        LanguageManager lang = GeyserConnect.instance().languageManager();
        
        session.sendForm(ModalForm.builder()
            .title(lang.get("ui.title.delete_server"))
            .content(lang.getMessage("ui.content.delete_server", server.title()))
            .button1(lang.get("ui.button.yes"))
            .button2(lang.get("ui.button.no"))
            .closedOrInvalidResultHandler(response -> {
                sendServerOptionsMenu(server);
            })
            .validResultHandler(response -> {
                if (response.clickedButtonId() == 0) {
                    ServerManager.removeServer(session, server);
                }
                sendEditServersMenu();
            }));
    }

    public void sendDirectConnectMenu() {
        LanguageManager lang = GeyserConnect.instance().languageManager();
        
        session.sendForm(CustomForm.builder()
            .title(lang.get("ui.title.direct_connect"))
            .input(lang.get("ui.label.ip"), "play.cubecraft.net")
            .input(lang.get("ui.label.port"), "25565", "25565")
            .toggle(lang.get("ui.label.online_mode"), true)
            .toggle(lang.get("ui.label.bedrock_server"), false)
            .closedOrInvalidResultHandler(response -> {
                sendMainMenu();
            })
            .validResultHandler(response -> {
                String ip = response.asInput(0);
                int port = Integer.parseInt(response.asInput(1));
                boolean onlineMode = response.asToggle(2);
                boolean geyserServer = response.asToggle(3);

                Server server = new Server(ip, port, onlineMode, geyserServer, null, null, ServerCategory.CUSTOM);
                Utils.sendToServer(session, originalPacketHandler, server);
            }));
    }
}
