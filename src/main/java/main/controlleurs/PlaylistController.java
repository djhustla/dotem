package main.controlleurs;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class PlaylistController {
/*
    @GetMapping("/api/playlist/dutch")
    public ResponseEntity<Resource> getDutchPlaylist() {
        Path path = Paths.get("C:/java/Security02/data/playlist/dutch.txt");
        Resource file = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(file);
    }

 */


/*
    @GetMapping("/api/playlist/fr")
    public ResponseEntity<Resource> getFrPlaylist() {
        Path path = Paths.get("C:/java/Security02/data/playlist/fr.txt");
        Resource file = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(file);
    }

 */






}
