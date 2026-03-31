package org.example.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Config {
    public long updateDelayTime;
    public long checkNewRequestTimeDelay;
    public String newRequestFileName;
    public String answerFileNamePrefix;
    public String answerAttachmentFileNamePrefix;
    public String dbFileName;
    public String botPassword;
    public String botUrl;
    public String botToken;
    public String mailDebug;
    public String mailImapHost;
    public int mailImapPort;
    public String mailImapsSslEnable;
    public String mailImapsSslProtocols;
    public long mailImapTimeout;
    public long mailImapConnectionTimeout;
    public String mailImapAuth;
    public String mailFolderToScan;
    public String mailUserName;
    public String mailPassword;

}
