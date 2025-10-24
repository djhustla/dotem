package main.controlleurs;


import main.modeles.MusicType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/music")
public class MusicController {

    @GetMapping("/types")
    public List<Map<String, String>> getAllMusicTypes() {
        return Arrays.stream(MusicType.values())
                .map(musicType -> Map.of(
                        "value", musicType.name(),
                        "label", formatMusicTypeLabel(musicType)
                ))
                .collect(Collectors.toList());
    }

    private String formatMusicTypeLabel(MusicType musicType) {
        switch (musicType) {
            case KPOP:
                return "K-Pop";
            case RAP_FRANCAIS:
                return "Rap Français";
            case RAP_AMERICAIN:
                return "Rap Américain";
            case LATINO:
                return "Latino";
            case COMMERCIAL:
                return "Commercial";
            case ELECTRO:
                return "Electro";
            case AFRO_SHATTA:
                return "Afro & Shatta";
            default:
                return musicType.name();
        }
    }}