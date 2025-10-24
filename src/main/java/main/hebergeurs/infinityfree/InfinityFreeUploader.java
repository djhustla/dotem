package main.hebergeurs.infinityfree;

import org.apache.commons.net.ftp.FTPClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class InfinityFreeUploader {

    private static final String SERVER = "ftpupload.net"; // Serveur pour 42web.io
    private static final int PORT = 21;
    private static final String USER = "if0_40185678"; // Votre username
    private static final String PASS = "PMQZ4clALeoxf"; // Votre mot de passe
    private static final String DOMAIN = "monappmusique.42web.io";

    /**
     * Upload un fichier vers le serveur FTP et retourne l'URL complète
     * @param localFilePath Chemin complet du fichier local à uploader
     * @param remoteFilePath Chemin distant sur le serveur (ex: "images/photo.jpg" ou "musique/titre.mp3")
     * @return L'URL complète du fichier uploadé, ou null en cas d'échec
     */
    public static String uploadFile(String localFilePath, String remoteFilePath) {
        FTPClient ftpClient = new FTPClient();

        try {
            // Connexion au serveur
            System.out.println("Tentative de connexion à " + SERVER + ":" + PORT);
            ftpClient.connect(SERVER, PORT);
            ftpClient.login(USER, PASS);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            // Vérifier la connexion
            if (!ftpClient.isConnected()) {
                System.out.println("Échec de connexion FTP");
                return null;
            }

            System.out.println("✅ Connecté à InfinityFree FTP");
            System.out.println("Utilisateur: " + USER);

            // Vérifier si le fichier local existe
            File localFile = new File(localFilePath);
            if (!localFile.exists()) {
                System.out.println("❌ Fichier local introuvable: " + localFilePath);
                return null;
            }

            System.out.println("📁 Upload du fichier: " + localFile.getName() + " (" + localFile.length() + " bytes)");
            System.out.println("🎯 Destination: " + remoteFilePath);

            // Créer les dossiers parents si nécessaire
            createParentDirectories(ftpClient, remoteFilePath);

            // Upload du fichier
            FileInputStream inputStream = new FileInputStream(localFile);

            // Chemin distant complet
            String remotePath = "/htdocs/" + remoteFilePath;
            boolean success = ftpClient.storeFile(remotePath, inputStream);
            inputStream.close();

            if (success) {
                System.out.println("✅ Fichier uploadé avec succès: " + remoteFilePath);

                // Générer l'URL d'accès
                String fileUrl = "http://" + DOMAIN + "/" + remoteFilePath;
                System.out.println("🌐 URL du fichier: " + fileUrl);

                return fileUrl;
            } else {
                System.out.println("❌ Échec de l'upload");
                // Vérifier la réponse du serveur
                System.out.println("Code de réponse: " + ftpClient.getReplyCode());
                System.out.println("Message: " + ftpClient.getReplyString());
                return null;
            }

        } catch (IOException e) {
            System.out.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                    System.out.println("🔌 Déconnexion FTP");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Crée les dossiers parents nécessaires pour le chemin distant
     */
    private static void createParentDirectories(FTPClient ftpClient, String remoteFilePath) throws IOException {
        // Extraire le chemin du dossier parent
        int lastSlash = remoteFilePath.lastIndexOf('/');
        if (lastSlash > 0) {
            String parentDirectory = remoteFilePath.substring(0, lastSlash);
            String[] directories = parentDirectory.split("/");

            String currentPath = "/htdocs";
            for (String dir : directories) {
                if (!dir.isEmpty()) {
                    currentPath += "/" + dir;
                    // Vérifier si le dossier existe déjà
                    if (!ftpClient.changeWorkingDirectory(currentPath)) {
                        // Créer le dossier s'il n'existe pas
                        if (ftpClient.makeDirectory(currentPath)) {
                            System.out.println("📁 Dossier créé: " + currentPath);
                        }
                    }
                }
            }
        }
    }

    // Méthode pour créer un dossier sur le serveur
    public static boolean createDirectory(String directoryName) {
        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(SERVER, PORT);
            ftpClient.login(USER, PASS);
            ftpClient.enterLocalPassiveMode();

            String remotePath = "/htdocs/" + directoryName;
            boolean success = ftpClient.makeDirectory(remotePath);

            if (success) {
                System.out.println("✅ Dossier créé: " + directoryName);
            } else {
                System.out.println("❌ Échec création dossier: " + directoryName);
            }
            return success;

        } catch (IOException e) {
            System.out.println("❌ Erreur création dossier: " + e.getMessage());
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Méthode pour lister les fichiers (utile pour debug)
    public static void listFiles() {
        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(SERVER, PORT);
            ftpClient.login(USER, PASS);
            ftpClient.enterLocalPassiveMode();

            String[] files = ftpClient.listNames("/htdocs/");
            System.out.println("📂 Fichiers sur le serveur:");
            if (files != null) {
                for (String file : files) {
                    System.out.println("  - " + file);
                }
            } else {
                System.out.println("  (aucun fichier)");
            }

        } catch (IOException e) {
            System.out.println("❌ Erreur listage: " + e.getMessage());
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Méthode pour tester
    public static void main(String[] args) {
        // Test de connexion et listage
        System.out.println("🧪 Test de connexion InfinityFree...");



        // Test d'upload avec la nouvelle signature
        String localFile = "C:\\Users\\Win\\Documents\\mes documents Doka\\k7 Flyers\\00.png";//"C:\\java\\Security02\\data\\photos\\01.png";
        String remoteFile = "images/012222-uploaded.png";
        String fileUrl = uploadFile(localFile, remoteFile);


        File testFile = new File(localFile);
        if (!testFile.exists()) {
            System.out.println("⚠️  Fichier local non trouvé: " + localFile);
            return;
        }

        System.out.println(" url1  " + fileUrl);

        /*
        // Appel de la nouvelle méthode qui retourne l'URL
        String fileUrl = uploadFile("C:\\java\\Security02\\data\\playlist\\dutch.txt", "playlist_list/dutch.txt");
        String fileUrl02 = uploadFile("C:\\java\\Security02\\data\\playlist\\us.txt", "playlist_list/us.txt");
        String fileUrl03 = uploadFile("C:\\java\\Security02\\data\\playlist\\fr.txt", "playlist_list/fr.txt");

         */

        if (fileUrl != null) {
            System.out.println("🎯 Upload réussi! URL: " + fileUrl);

            // Relister pour voir le nouveau fichier
            listFiles();

            // Exemples d'utilisation
            System.out.println("\n📋 Exemples d'utilisation:");
            System.out.println("String url1 = uploadFile(\"photo.jpg\", \"images/ma-photo.jpg\");");
            System.out.println("String url2 = uploadFile(\"musique.mp3\", \"musique/titre.mp3\");");
            System.out.println("String url3 = uploadFile(\"document.pdf\", \"docs/mon-doc.pdf\");");
        } else {
            System.out.println("❌ Échec de l'upload");
        }
    }
}