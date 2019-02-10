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

OutputBaseFilename=OpenEstate-ImmoServer-{#Version}.win64.setup
Compression=lzma2
SolidCompression=no

MinVersion=6.0
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
SetupMutex=OpenEstateImmoServerSetup,Global\OpenEstateImmoServerSetup

DefaultDirName={pf}\OpenEstate-ImmoServer
DefaultGroupName=OpenEstate-ImmoServer

Uninstallable=yes
UninstallDisplayIcon={app}\share\icons\ImmoServer.ico

DisableWelcomePage=no
AllowNoIcons=yes
WizardSmallImageFile=logo-100.bmp,logo-125.bmp,logo-150.bmp,logo-175.bmp,logo-200.bmp,logo-225.bmp,logo-250.bmp
WizardImageFile=sidebar-100.bmp,sidebar-125.bmp,sidebar-150.bmp,sidebar-175.bmp,sidebar-200.bmp,sidebar-225.bmp,sidebar-250.bmp


; [Tasks]
; Name: "desktopicon"; \
;     Description: "{cm:CreateDesktopIcon}"; \
;     GroupDescription: "{cm:AdditionalIcons}"


[Files]
Source: "{#Package}\*"; \
    DestDir: "{app}"; \
    Flags: recursesubdirs


[Icons]
Name: "{group}\{cm:StartImmoServer}"; \
    Filename: "{app}\bin\Start.exe"
Name: "{group}\{cm:Management}\{cm:ManagementSslInit}"; \
    Filename: "{app}\bin\SslInit.exe"
Name: "{group}\{cm:Management}\{cm:ManagementConsole}"; \
    Filename: "{app}\bin\ManagerConsole.exe"
Name: "{group}\{cm:Management}\{cm:ManagementTool}"; \
    Filename: "{app}\bin\ManagerTool.exe"
Name: "{group}\{cm:Management}\{cm:ManagementBackup}"; \
    Filename: "{app}\bin\ManagerBackup.exe"; \
    Parameters: "-wait"
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
Name: "{group}\{cm:UninstallProgram,ImmoServer}"; \
    Filename: "{uninstallexe}"; \
    WorkingDir: "{app}"
; Name: "{commondesktop}\OpenEstate-ImmoServer"; \
;     Filename: "{app}\bin\Start.exe"; \
;     Tasks: desktopicon


[UninstallDelete]
Type: filesandordirs; \
    Name: "{app}\etc\ssl"


[UninstallRun]
Filename: "{app}\bin\ServiceStop.bat"; \
    Parameters: "/q"; \
    WorkingDir: "{app}\bin"; \
    RunOnceId: "StopService"; \
    Flags: runascurrentuser runhidden
Filename: "{app}\bin\ServiceUninstall.bat"; \
    Parameters: "/q"; \
    WorkingDir: "{app}\bin"; \
    RunOnceId: "UninstallService"; \
    Flags: runascurrentuser runhidden


[Languages]
Name: "en"; \
    MessagesFile: "compiler:Default.isl"
Name: "de"; \
    MessagesFile: "compiler:Languages\German.isl"


[CustomMessages]
StartImmoServer=Start ImmoServer manually
Management=Management
ManagementSslInit=Create SSL certificate
ManagementConsole=Open database console
ManagementTool=Open database tool
ManagementBackup=Create database backup
Service=ImmoServer as a service
ServiceInstall=Install ImmoServer service
ServiceUninstall=Uninstall ImmoServer service
ServiceStart=Start ImmoServer service
ServiceStop=Stop ImmoServer service
ServiceManage=Manage ImmoServer service

de.StartImmoServer=ImmoServer manuell starten
de.Management=Verwaltung
de.ManagementSslInit=SSL-Zertifikat erzeugen
de.ManagementConsole=Datenbank-Konsole öffnen
de.ManagementTool=Datenbank-Tool öffnen
de.ManagementBackup=Datenbank sichern
de.Service=Dienst
de.ServiceInstall=ImmoServer-Dienst installieren
de.ServiceUninstall=ImmoServer-Dienst deinstallieren
de.ServiceStart=ImmoServer-Dienst starten
de.ServiceStop=ImmoServer-Dienst stoppen
de.ServiceManage=ImmoServer-Dienst verwalten
