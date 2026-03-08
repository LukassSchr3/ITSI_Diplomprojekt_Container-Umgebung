import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

export interface ExerciseStep {
  nr: number;
  title: string;
  description: string;
  hint?: string;
}

export interface ExerciseDetail {
  id: string;
  title: string;
  category: string;
  difficulty: 'Leicht' | 'Mittel' | 'Schwer';
  duration: string;
  tools: string[];
  goal: string;
  background: string;
  steps: ExerciseStep[];
  deliverable: string;
}

const EXERCISE_DETAILS: Record<string, ExerciseDetail> = {
  '9.1': {
    id: '9.1',
    title: 'ITSI 9.1 – Netzwerkforensik',
    category: 'Forensik',
    difficulty: 'Mittel',
    duration: '90 min',
    tools: ['Wireshark', 'tcpdump', 'NetworkMiner', 'tshark'],
    goal: 'Analysiere einen verdächtigen Netzwerkmitschnitt und rekonstruiere den Ablauf eines Angriffs anhand von Paketdaten.',
    background: `In diesem Szenario wurde ein internes Firmennetzwerk kompromittiert. Der Sicherheitsverantwortliche hat einen PCAP-Mitschnitt des betroffenen Segments erstellt. Deine Aufgabe ist es, den Angriff zu rekonstruieren, den Angreifer zu identifizieren und die exfiltrierten Daten zu finden. Netzwerkforensik ist eine Kerndisziplin der digitalen Forensik – Pakete lügen nicht, wenn man weiß, wie man sie liest.`,
    steps: [
      {
        nr: 1,
        title: 'PCAP-Datei öffnen',
        description: 'Öffne die bereitgestellte Datei capture.pcap in Wireshark. Verschaffe dir einen ersten Überblick über die Protokollverteilung unter „Statistics → Protocol Hierarchy".',
        hint: 'Achte auf ungewöhnliche Protokollanteile – ein hoher UDP-Anteil oder unbekannte Ports können ein Hinweis sein.'
      },
      {
        nr: 2,
        title: 'Verdächtige Verbindungen filtern',
        description: 'Nutze den Wireshark-Filter tcp.flags.syn == 1 && tcp.flags.ack == 0 um alle initialen TCP-Verbindungsversuche anzuzeigen. Identifiziere externe IP-Adressen, die auffällig viele Verbindungen aufbauen.',
        hint: 'Der Filter ip.addr == <verdächtige-IP> hilft dir, den Traffic einer bestimmten Quelle zu isolieren.'
      },
      {
        nr: 3,
        title: 'HTTP-Traffic rekonstruieren',
        description: 'Filtere auf http und nutze „Follow → HTTP Stream" um die Inhalte gesendeter HTTP-Requests vollständig zu lesen. Suche nach POST-Requests die ungewöhnlich große Payloads enthalten.',
        hint: 'Unter „File → Export Objects → HTTP" kannst du übertragene Dateien direkt extrahieren.'
      },
      {
        nr: 4,
        title: 'DNS-Anfragen analysieren',
        description: 'Filtere auf dns und suche nach Domain-Namen, die ungewöhnlich lang sind oder Base64-kodierte Subdomains enthalten. DNS-Tunneling ist eine häufige Exfiltrationsmethode.',
        hint: 'tshark -r capture.pcap -Y "dns" -T fields -e dns.qry.name | sort | uniq -c | sort -rn'
      },
      {
        nr: 5,
        title: 'Zeitlinie des Angriffs erstellen',
        description: 'Erstelle basierend auf deinen Erkenntnissen eine chronologische Zeitlinie: Wann fand der erste Zugriff statt? Welche Systeme wurden kontaktiert? Welche Daten wurden exfiltriert? Halte alles in einem Bericht fest.'
      }
    ],
    deliverable: 'Erstelle einen forensischen Bericht (mind. 1 Seite) mit: Angreifer-IP, verwendete Protokolle, exfiltrierte Daten, Zeitstempel der Ereignisse und deiner Einschätzung des Angriffsvektors.'
  },

  '9.2': {
    id: '9.2',
    title: 'ITSI 9.2 – Memory Forensics',
    category: 'Forensik',
    difficulty: 'Schwer',
    duration: '120 min',
    tools: ['Volatility 3', 'strings', 'grep', 'hexdump'],
    goal: 'Analysiere einen RAM-Dump eines kompromittierten Windows-Systems mit Volatility und identifiziere laufende Schadprozesse sowie gestohlene Credentials.',
    background: `Ein Endpoint-Detection-System hat auf einem Windows-10-Rechner eines Mitarbeiters verdächtiges Verhalten gemeldet. Bevor das System abgeschaltet wurde, konnte ein vollständiger RAM-Dump erstellt werden. Memory Forensics ermöglicht es, flüchtige Artefakte zu analysieren, die auf der Festplatte nicht existieren: laufende Prozesse, Netzwerkverbindungen, entschlüsselte Passwörter und injizierter Code.`,
    steps: [
      {
        nr: 1,
        title: 'System-Profil bestimmen',
        description: 'Führe vol3 -f memory.dmp windows.info aus um das Betriebssystem und die Version zu identifizieren. Diese Information ist notwendig für alle weiteren Analysen.',
        hint: 'Bei Volatility 3 ist kein manuelles Profil mehr nötig – das Tool erkennt das OS automatisch.'
      },
      {
        nr: 2,
        title: 'Prozessliste analysieren',
        description: 'Nutze vol3 -f memory.dmp windows.pstree um die Prozesshierarchie anzuzeigen. Suche nach Prozessen die von ungewöhnlichen Parent-Prozessen gestartet wurden (z.B. cmd.exe gestartet von word.exe).',
        hint: 'Vergleiche die Prozessliste mit einer normalen Windows-Instanz – unbekannte Namen oder doppelte svchost.exe Instanzen sind verdächtig.'
      },
      {
        nr: 3,
        title: 'Netzwerkverbindungen prüfen',
        description: 'Führe vol3 -f memory.dmp windows.netstat aus. Identifiziere aktive und kürzlich geschlossene Verbindungen zu externen IP-Adressen. Recherchiere die IPs auf Virustotal.',
        hint: 'ESTABLISHED-Verbindungen auf Port 4444 oder 1337 sind klassische Reverse-Shell-Indikatoren.'
      },
      {
        nr: 4,
        title: 'Credentials extrahieren',
        description: 'Nutze vol3 -f memory.dmp windows.hashdump um NTLM-Passwort-Hashes zu extrahieren. Versuche die Hashes mit einem Online-Lookup (z.B. crackstation.net) zu knacken.',
        hint: 'Achte auch auf windows.lsadump – dort können Plaintext-Credentials gespeichert sein.'
      },
      {
        nr: 5,
        title: 'Injizierten Code finden',
        description: 'Verwende vol3 -f memory.dmp windows.malfind um Speicherbereiche mit ausführbarem Code zu finden, die nicht zu einer bekannten DLL gehören. Dumpe verdächtige Bereiche und analysiere sie mit einem Disassembler.',
        hint: 'MZ-Header (4D 5A) am Anfang eines gedumpten Bereichs deuten auf eine eingebettete PE-Datei hin.'
      }
    ],
    deliverable: 'Dokumentiere: Name des Schadprozesses + PID, externe C2-IP-Adresse, gefundene Credentials (gehasht), Injektionsmethode und deine Einschätzung der Schadsoftware-Familie.'
  },

  '9.3': {
    id: '9.3',
    title: 'ITSI 9.3 – Malware-Analyse (Android)',
    category: 'Malware',
    difficulty: 'Mittel',
    duration: '100 min',
    tools: ['APKTool', 'JADX', 'ADB', 'MobSF', 'dex2jar'],
    goal: 'Analysiere eine verdächtige Android-APK-Datei, identifiziere Schadfunktionen im Quellcode und rekonstruiere die Kommunikation mit einem C2-Server.',
    background: `Ein Nutzer hat eine APK außerhalb des Play Stores installiert und bemerkt danach ungewöhnlichen Datenverbrauch sowie unbekannte Hintergrundprozesse. Die APK-Datei wurde sichergestellt. Deine Aufgabe ist die statische und dynamische Analyse dieser Datei. Android-Malware tarnt sich häufig als legitime App – nur eine gründliche Codeanalyse deckt die wahren Absichten auf.`,
    steps: [
      {
        nr: 1,
        title: 'APK entpacken und Manifest analysieren',
        description: 'Nutze apktool d suspicious.apk -o output/ um die APK zu dekompilieren. Öffne AndroidManifest.xml und analysiere alle angeforderten Permissions. Besonders kritisch: SEND_SMS, READ_CONTACTS, RECORD_AUDIO, CAMERA.',
        hint: 'Das Manifest verrät oft schon den Zweck einer Malware – eine Taschenlampen-App die Microphone-Zugriff braucht ist verdächtig.'
      },
      {
        nr: 2,
        title: 'Quellcode dekompilieren',
        description: 'Öffne die APK in JADX-GUI. Navigiere durch die Package-Struktur und suche nach Klassen die Netzwerk-Operationen, SMS-Versand oder Gerätedaten-Sammlung durchführen.',
        hint: 'Suche in JADX nach Strings wie "http://", "sms", "getDeviceId", "getSubscriberId" um schnell relevante Code-Stellen zu finden.'
      },
      {
        nr: 3,
        title: 'C2-Kommunikation identifizieren',
        description: 'Suche im dekompilierten Code nach fest kodierten IP-Adressen oder Domains. Analysiere wie die App Daten überträgt: HTTP POST? Base64-kodiert? Verschlüsselt? Rekonstruiere das Kommunikationsprotokoll.',
        hint: 'Strings können verschleiert sein (z.B. rückwärts gespeichert oder XOR-kodiert). Achte auf String-Manipulations-Methoden im Code.'
      },
      {
        nr: 4,
        title: 'Dynamische Analyse im Emulator',
        description: 'Starte einen Android-Emulator via AVD und installiere die APK mit adb install suspicious.apk. Überwache den Netzwerkverkehr mit einem Man-in-the-Middle-Proxy (z.B. mitmproxy) und beobachte welche Daten die App sendet.',
        hint: 'Stelle das Proxy-Zertifikat als vertrauenswürdig ein, sonst blockiert die App HTTPS-Verbindungen.'
      },
      {
        nr: 5,
        title: 'IOCs extrahieren',
        description: 'Erstelle eine Liste aller Indicators of Compromise (IOCs): C2-Domains/IPs, Package-Name, SHA256-Hash der APK, verdächtige Permissions, extrahierte Strings. Diese IOCs können für Threat Intelligence verwendet werden.',
        hint: 'Den SHA256-Hash erhältst du mit: sha256sum suspicious.apk'
      }
    ],
    deliverable: 'Erstelle einen Malware-Analysebericht mit: App-Zweck/Schadfunktion, C2-Adresse, gestohlene Daten, alle IOCs und einer Empfehlung für Abwehrmaßnahmen.'
  }
};

@Component({
  selector: 'app-exercise-detail',
  imports: [CommonModule, RouterLink],
  templateUrl: './exercise-detail.component.html',
  styleUrl: './exercise-detail.component.css'
})
export class ExerciseDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  exercise = signal<ExerciseDetail | null>(null);
  notFound = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id || !EXERCISE_DETAILS[id]) {
      this.notFound.set(true);
      return;
    }
    this.exercise.set(EXERCISE_DETAILS[id]);
  }

  startLiveEnv(): void {
    this.router.navigate(['/image', 1]);
  }

  goBack(): void {
    this.router.navigate(['/exercises']);
  }
}
