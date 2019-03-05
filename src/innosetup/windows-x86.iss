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

OutputBaseFilename=OpenEstate-ImmoServer-{#Version}.windows-x86
Compression=lzma2
SolidCompression=no

MinVersion=6.0
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
    Filename: "{app}\bin\Start.exe"; \
    WorkingDir: "{app}\bin"; \
    Comment: "{cm:StartImmoServerComment}"
Name: "{group}\{cm:VisitWebsite}"; \
    Filename: "https://openestate.org"
Name: "{group}\{cm:VisitManual}"; \
    Filename: "https://manual.openestate.org"
Name: "{group}\{cm:VisitRepository}"; \
    Filename: "https://github.com/OpenEstate/OpenEstate-Tool-Server"
Name: "{group}\{cm:Management}\{cm:ManagementSslInit}"; \
    Filename: "{app}\bin\SslInit.exe"; \
    WorkingDir: "{app}\bin"; \
    Comment: "{cm:ManagementSslInitComment}"
Name: "{group}\{cm:Management}\{cm:ManagementConsole}"; \
    Filename: "{app}\bin\ManagerConsole.exe"; \
    WorkingDir: "{app}\bin"; \
    Comment: "{cm:ManagementConsoleComment}"
Name: "{group}\{cm:Management}\{cm:ManagementTool}"; \
    Filename: "{app}\bin\ManagerTool.exe"; \
    WorkingDir: "{app}\bin"; \
    Comment: "{cm:ManagementToolComment}"
Name: "{group}\{cm:Management}\{cm:ManagementBackup}"; \
    Filename: "{app}\bin\ManagerBackup.exe"; \
    WorkingDir: "{app}\bin"; \
    Parameters: "-wait"; \
    Comment: "{cm:ManagementBackupComment}"
Name: "{group}\{cm:Management}\{cm:ManagementDataFolder}"; \
    Filename: "{win}\explorer.exe"; \
    WorkingDir: "{win}"; \
    Parameters: "%USERPROFILE%\OpenEstate-ImmoServer"; \
    Comment: "{cm:ManagementDataFolderComment}"
Name: "{group}\{cm:Service}\{cm:ServiceInstall}"; \
    Filename: "{app}\bin\ServiceInstall.bat"; \
    WorkingDir: "{app}\bin"; \
    IconFilename: "{app}\share\icons\Manager.ico"; \
    Comment: "{cm:ServiceInstallComment}"
Name: "{group}\{cm:Service}\{cm:ServiceUninstall}"; \
    Filename: "{app}\bin\ServiceUninstall.bat"; \
    WorkingDir: "{app}\bin"; \
    IconFilename: "{app}\share\icons\Manager.ico"; \
    Comment: "{cm:ServiceUninstallComment}"
Name: "{group}\{cm:Service}\{cm:ServiceStart}"; \
    Filename: "{app}\bin\ServiceStart.bat"; \
    WorkingDir: "{app}\bin"; \
    IconFilename: "{app}\share\icons\Manager.ico"; \
    Comment: "{cm:ServiceStartComment}"
Name: "{group}\{cm:Service}\{cm:ServiceStop}"; \
    Filename: "{app}\bin\ServiceStop.bat"; \
    WorkingDir: "{app}\bin"; \
    IconFilename: "{app}\share\icons\Manager.ico"; \
    Comment: "{cm:ServiceStopComment}"
Name: "{group}\{cm:Service}\{cm:ServiceManage}"; \
    Filename: "{app}\bin\service\OpenEstate-ImmoServer.exe"; \
    WorkingDir: "{app}\bin\service"; \
    Comment: "{cm:ServiceManageComment}"
Name: "{group}\{cm:UninstallProgram,ImmoServer}"; \
    Filename: "{uninstallexe}"; \
    WorkingDir: "{app}"
; Name: "{commondesktop}\{cm:StartImmoServer}"; \
;     Filename: "{app}\bin\Start.exe"; \
;     WorkingDir: "{app}\bin"; \
;     Comment: "{cm:StartImmoServerComment}"; \
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

;
; English custom messages...
;

StartImmoServer=Start ImmoServer manually
StartImmoServerComment=Start OpenEstate-ImmoServer in foreground.
VisitWebsite=Visit openestate.org
VisitManual=Visit online manual
VisitRepository=Visit source code repository
Management=Management
ManagementSslInit=Create SSL certificate
ManagementSslInitComment=Create a SSL certificate for encrypted connections.
ManagementConsole=Open database console
ManagementConsoleComment=Open a console application for database management.
ManagementTool=Open database tool
ManagementToolComment=Open a graphical application for database management.
ManagementBackup=Create database backup
ManagementBackupComment=Create a backup of the currently running database.
ManagementDataFolder=Open data folder
ManagementDataFolderComment=Open the data folder of OpenEstate-ImmoServer.
Service=Service
ServiceInstall=Install ImmoServer service
ServiceInstallComment=Install a service for OpenEstate-ImmoServer.
ServiceUninstall=Uninstall ImmoServer service
ServiceUninstallComment=Uninstall the service for OpenEstate-ImmoServer.
ServiceStart=Start ImmoServer service
ServiceStartComment=Start the service for OpenEstate-ImmoServer.
ServiceStop=Stop ImmoServer service
ServiceStopComment=Stop the service for OpenEstate-ImmoServer.
ServiceManage=Manage ImmoServer service
ServiceManageComment=Manage the service for OpenEstate-ImmoServer.


;
; German custom messages...
;

de.StartImmoServer=ImmoServer manuell starten
de.StartImmoServerComment=OpenEstate-ImmoServer im Vordergrund starten.
de.VisitWebsite=Openestate.org öffnen
de.VisitManual=Online-Handbuch öffnen
de.VisitRepository=Repository mit Quelltexten öffnen
de.Management=Verwaltung
de.ManagementSslInit=SSL-Zertifikat erzeugen
de.ManagementSslInitComment=Ein SSL-Zertifikat für verschlüsselte Verbindungen erzeugen.
de.ManagementConsole=Datenbank-Konsole öffnen
de.ManagementConsoleComment=Ein Konsole-Programm zur Verwaltung der Datenbank öffnen.
de.ManagementTool=Datenbank-Tool öffnen
de.ManagementToolComment=Ein grafisches Programm zur Verwaltung der Datenbank öffnen.
de.ManagementBackup=Datenbank sichern
de.ManagementBackupComment=Eine Sicherung der aktuell gestarteten Datenbank erzeugen.
de.ManagementDataFolder=Datenverzeichnis öffnen
de.ManagementDataFolderComment=Das Verzeichnis mit den Daten von OpenEstate-ImmoServer öffnen.
de.Service=Dienst
de.ServiceInstall=ImmoServer-Dienst installieren
de.ServiceInstallComment=OpenEstate-ImmoServer als Dienst im Betriebssystem installieren.
de.ServiceUninstall=ImmoServer-Dienst deinstallieren
de.ServiceUninstallComment=Den Dienst von OpenEstate-ImmoServer aus dem Betriebssystem entfernen.
de.ServiceStart=ImmoServer-Dienst starten
de.ServiceStartComment=Den Dienst von OpenEstate-ImmoServer starten.
de.ServiceStop=ImmoServer-Dienst stoppen
de.ServiceStopComment=Den Dienst von OpenEstate-ImmoServer stoppen.
de.ServiceManage=ImmoServer-Dienst verwalten
de.ServiceManageComment=Den Dienst von OpenEstate-ImmoServer verwalten.
