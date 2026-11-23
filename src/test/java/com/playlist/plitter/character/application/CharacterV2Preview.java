package com.playlist.plitter.character.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.character.application.dto.*;
import java.nio.file.*;
import java.util.*;

/** Exports test fixtures only: no live database, OpenAI or S3 access. */
public class CharacterV2Preview {
    public static void main(String[] args) throws Exception {
        var catalog=new CharacterConceptCatalog();
        var analyzer=new MusicProfileAnalyzer();
        var generator=new CharacterConceptGenerator(catalog);
        var prompts=new CharacterEditSpecGenerator(catalog);
        var metadata=new CharacterResultMetadataGenerator(catalog);
        Object[][] fixtures={
                {"ballad",.28,.30,"괜히 센치한 스타"},
                {"acoustic",.25,.80,"유난히 말랑한 스타"},
                {"rock",.83,.25,"쓸데없이 비장한 스타"},
                {"dance",.82,.80,"묘하게 잔망스러운 스타"}};
        String[] requiredTags={"#새벽감성","#이불속콘서트","#플리과몰입","#둠칫둠칫"};
        List<Map<String,Object>> samples=new ArrayList<>();
        for(int i=0;i<fixtures.length;i++) {
            var f=fixtures[i];
            String summary="{\"trackCount\":5,\"energyCount\":5,\"valenceCount\":5,\"avgEnergy\":"+f[1]
                    +",\"avgValence\":"+f[2]+",\"primaryGenre\":\""+f[0]+"\"}";
            CharacterConcept selected=null;
            for(long seed=i*100L;seed<i*100L+1024;seed++) {
                var concept=generator.generate(analyzer.analyze(summary),seed,List.of());
                if(metadata.generate(concept).characterName().equals(f[3]) && metadata.generate(concept).tags().contains(requiredTags[i])) { selected=concept;break; }
            }
            if(selected==null)throw new IllegalStateException("Missing name fixture");
            samples.add(Map.of("id",i+1,"fixture",new ObjectMapper().readTree(summary),"concept",selected,
                    "metadata",metadata.generate(selected),"prompt",prompts.generate(selected).promptText()));
        }
        Path directory=Path.of(args.length>0?args[0]:"output/character-v2");Files.createDirectories(directory);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(directory.resolve("samples.json").toFile(),samples);
        System.out.println("Exported "+samples.size()+" local V2 character fixtures to "+directory.resolve("samples.json"));
    }
}
