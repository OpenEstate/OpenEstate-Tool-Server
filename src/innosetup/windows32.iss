[Setup]
AppId=OpenEstateImmoServer
AppName=OpenEstate-ImmoServer
AppPublisher=OpenEstate.org
AppPublisherURL=https://openestate.org/
AppSupportURL=https://openestate.org/support/tickets
AppUpdatesURL=https://openestate.org/downloads/openestate-immoserver
AppVersion={#VersionNumber}
AppVerName=OpenEstate-ImmoServer {#Version}
AppCopyright=(C) 2009-2019 OpenEstate.org
VersionInfoVersion={#VersionNumber}
VersionInfoTextVersion={#Version}

OutputBaseFilename=OpenEstate-ImmoServer-{#Version}.win32.setup
Compression=lzma2
SolidCompression=no

MinVersion=6.0

DefaultDirName={pf}\OpenEstate-ImmoServer
DefaultGroupName=OpenEstate-ImmoServer

Uninstallable=yes
UninstallDisplayIcon={app}\share\icons\ImmoServer.ico

DisableWelcomePage=no
AllowNoIcons=yes

SetupMutex=OpenEstateImmoServerSetup,Global\OpenEstateImmoServerSetup


[Tasks]
Name: "desktopicon"; \
    Description: "{cm:CreateDesktopIcon}"; \
    GroupDescription: "{cm:AdditionalIcons}"


[Files]
Source: "{#Package}\*"; \
    DestDir: "{app}"; \
    Flags: recursesubdirs


[Icons]
Name: "{group}\ImmoServer"; \
    Filename: "{app}\bin\Start.exe"
Name: "{group}\{cm:Service}\{cm:ServiceInstall}"; \
    Filename: "{app}\bin\ServiceInstall.bat"
Name: "{group}\{cm:Service}\{cm:ServiceUninstall}"; \
    Filename: "{app}\bin\ServiceUninstall.bat"
Name: "{group}\{cm:Service}\{cm:ServiceStart}"; \
    Filename: "{app}\bin\ServiceStart.bat"
Name: "{group}\{cm:Service}\{cm:ServiceStop}"; \
    Filename: "{app}\bin\ServiceStop.bat"
Name: "{group}\{cm:Service}\{cm:ServiceManage}"; \
    Filename: "{app}\bin\service\OpenEstate-ImmoServer.exe"
Name: "{group}\{cm:UninstallProgram,ImmoTool}"; \
    Filename: "{uninstallexe}"; \
    WorkingDir: "{app}"
Name: "{commondesktop}\OpenEstate-ImmoServer"; \
    Filename: "{app}\bin\Start.exe"; \
    Tasks: desktopicon


[Languages]
Name: "en"; \
    MessagesFile: "compiler:Default.isl"
Name: "de"; \
    MessagesFile: "compiler:Languages\German.isl"


[CustomMessages]
Service=Dienst
ServiceInstall=Install service
ServiceUninstall=Uninstall service
ServiceStart=Start service
ServiceStop=Stop service
ServiceManage=Manage service

de.Service=Dienst
de.ServiceInstall=Dienst installieren
de.ServiceUninstall=Dienst deinstallieren
de.ServiceStart=Dienst starten
de.ServiceStop=Dienst stoppen
de.ServiceManage=Dienst verwalten
