package com.playlist.plitter.character.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.auth.domain.entity.UserEntity;
import com.playlist.plitter.auth.domain.repository.UserRepository;
import com.playlist.plitter.character.application.port.*;
import com.playlist.plitter.character.application.port.dto.*;
import com.playlist.plitter.character.domain.entity.CharacterEntity;
import com.playlist.plitter.character.domain.repository.CharacterRepository;
import com.playlist.plitter.character.infrastructure.RecommendationMusicFeatureClient;
import com.playlist.plitter.character.application.dto.CharacterResultMetadata;
import com.playlist.plitter.character.application.dto.CharacterConcept;
import com.playlist.plitter.character.application.dto.MusicProfile;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import com.playlist.plitter.recommendations.domain.entity.RecommendationsEntity;
import com.playlist.plitter.recommendations.domain.repository.RecommendationsRepository;
import com.playlist.plitter.track.domain.entity.*;
import com.playlist.plitter.track.domain.repository.*;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CharacterV2PersistenceTest {
    private AnnotationConfigApplicationContext context;
    private final ObjectMapper mapper = new ObjectMapper();
    @BeforeEach void open() { context = new AnnotationConfigApplicationContext(Config.class); }
    @AfterEach void close() { if (context != null) context.close(); }

    @Configuration
    @EnableJpaAuditing
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses={TrackRepository.class, UserRepository.class,
            PlaylistRepository.class, RecommendationsRepository.class, CharacterRepository.class})
    @Import({CharacterService.class, MusicProfileAnalyzer.class, CharacterConceptCatalog.class,
            CharacterConceptGenerator.class, CharacterEditSpecGenerator.class,
            CharacterResultMetadataGenerator.class, LegacyCharacterResultMetadataGenerator.class,
            LegacyCharacterEditSpecGenerator.class, RecommendationMusicFeatureClient.class})
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                    + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var f = new LocalContainerEntityManagerFactoryBean(); f.setDataSource(ds);
            f.setPackagesToScan("com.playlist.plitter.track.domain.entity", "com.playlist.plitter.auth.domain.entity",
                    "com.playlist.plitter.playlist.domain.entity", "com.playlist.plitter.recommendations.domain.entity",
                    "com.playlist.plitter.character.domain.entity");
            f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return f;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory f) { return new JpaTransactionManager(f); }
        @Bean BaseCharacterClient baseCharacterClient() { return () -> new BaseCharacterImage("test", "classpath:base_character.png"); }
        @Bean CharacterImageEditClient characterImageEditClient() { return request -> "https://fixture.local/generated.png"; }
        @Bean ImageStorageClient imageStorageClient() {
            return new ImageStorageClient() {
                public String storeCharacterImage(Long id, String source) { return "https://fixture.local/" + UUID.randomUUID() + ".png"; }
                public DownloadUrlResult createDownloadUrl(String image) { return new DownloadUrlResult(image+"?download", LocalDateTime.now().plusMinutes(5)); }
            };
        }
    }

    @Test void storedLlmResultsStayUnchangedAndNewGenerationUsesCatalog() throws Exception {
        var fixture = fixture(false);
        var service = context.getBean(CharacterService.class);
        var oldConcept = new CharacterConcept(2, "v2", "v2", "v2", 17,
                new MusicProfile(MusicProfile.Mood.LOW_LOW, "BALLAD", .28, .3, 5, 5, 5),
                "soft_smile", "listening", "music_player", "none", "LOW_LOW:0",
                List.of("새벽감성", "창가플리", "과몰입중"));
        var oldMetadata = new CharacterResultMetadata("전주부터 사연 있는 스타", List.of("#발라드", "#새벽감성", "#과몰입"));
        String storedMetadata = mapper.writeValueAsString(oldMetadata);
        var repository = context.getBean(CharacterRepository.class);
        var old = repository.save(CharacterEntity.create(
                context.getBean(PlaylistRepository.class).findById(fixture.id()).orElseThrow(), 1,
                "https://fixture.local/old.png", "old", "{}", mapper.writeValueAsString(oldConcept), storedMetadata));
        assertEquals(oldMetadata.characterName(), service.getCharacter(fixture.id(), fixture.owner()).characterName());
        assertEquals(oldMetadata.tags(), service.getCharacterDownloadUrl(fixture.id(), fixture.owner()).tags());
        var next = service.createCharacter(fixture.id(), fixture.owner());
        assertNotEquals(oldMetadata.characterName(), next.characterName());
        assertNotEquals(new HashSet<>(oldMetadata.tags().subList(1, 3)), new HashSet<>(next.tags().subList(1, 3)));
        assertTrue(context.getBean(CharacterConceptCatalog.class).mood(MusicProfile.Mood.LOW_LOW).names().contains(next.characterName()));
        assertEquals("#발라드", next.tags().get(0));
        assertEquals(storedMetadata, repository.findById(old.getId()).orElseThrow().getResultMetadataJson());
    }

    @Test void generationPersistsConceptAndMetadataAndAllReadsAgree() throws Exception {
        var fixture = fixture(false);
        var service = context.getBean(CharacterService.class);
        var response = service.createCharacter(fixture.id(), fixture.owner());
        var saved = context.getBean(CharacterRepository.class).findById(response.characterId()).orElseThrow();
        assertEquals(2, mapper.readTree(saved.getGenerationSpecJson()).path("schemaVersion").asInt());
        assertEquals("#발라드", response.tags().get(0));
        assertEquals(response.characterName(), mapper.readTree(saved.getResultMetadataJson()).path("characterName").asText());
        assertEquals(response.characterName(), service.getCharacter(fixture.id(),fixture.owner()).characterName());
        assertEquals(response.tags(), service.getCharacterDownloadUrl(fixture.id(),fixture.owner()).tags());
        assertEquals(1, saved.getVersion());
        var next = service.createCharacter(fixture.id(), fixture.owner());
        var second = context.getBean(CharacterRepository.class).findById(next.characterId()).orElseThrow();
        assertEquals(2,second.getVersion());
        assertNotEquals(response.characterName(),next.characterName());
        var firstSpec=mapper.readTree(saved.getGenerationSpecJson());
        var secondSpec=mapper.readTree(second.getGenerationSpecJson());
        assertFalse(List.of("expressionId","poseId","propId","marksId").stream()
                .allMatch(k -> firstSpec.path(k).equals(secondSpec.path(k))));
        assertEquals(5, mapper.readTree(second.getFeatureSummaryJson()).path("validTrackCount").asInt());
    }

    @Test void duplicateTrackRecommendationsDoNotInflateConfidence() throws Exception {
        var fixture=fixture(true);
        var response=context.getBean(CharacterService.class).createCharacter(fixture.id(),fixture.owner());
        var saved=context.getBean(CharacterRepository.class).findById(response.characterId()).orElseThrow();
        var summary=mapper.readTree(saved.getFeatureSummaryJson());
        assertEquals(1,summary.path("trackCount").asInt());
        assertEquals(1,summary.path("energyCount").asInt());
        assertEquals("UNKNOWN",mapper.readTree(saved.getGenerationSpecJson()).path("profile").path("mood").asText());
    }

    @Test void oldCharactersKeepLegacyNames() {
        var fixture=fixture(false);
        context.getBean(CharacterRepository.class).save(CharacterEntity.create(
                context.getBean(PlaylistRepository.class).findById(fixture.id()).orElseThrow(),1,"https://fixture.local/old.png","old",
                "{\"avgEnergy\":0.78,\"avgValence\":0.72,\"primaryGenre\":\"dance\"}"));
        assertEquals("댄스 스텝 스타",context.getBean(CharacterService.class).getCharacter(fixture.id(),fixture.owner()).characterName());
    }

    @Test void missingValuesStayNullAndInvalidValuesAreExcluded() throws Exception {
        var f=fixture(false);
        var features=context.getBean(TrackFeatureRepository.class);
        features.deleteAll();
        var track=context.getBean(TrackRepository.class).findAll().get(0);
        features.save(TrackFeatureEntity.builder().track(track).energy(new BigDecimal("1.20"))
                .valence(null).genre("balanced").featureSource("LASTFM_EMPTY").build());
        var json=mapper.readTree(context.getBean(MusicFeatureClient.class).collectFeatures(f.id()));
        assertTrue(json.path("avgEnergy").isNull()); assertTrue(json.path("avgValence").isNull());
        assertEquals(1,json.path("invalidEnergyCount").asInt());
        assertEquals(0,json.path("validTrackCount").asInt());
        assertEquals("unknown",json.path("primaryGenre").asText());
    }

    @Test void aliasesMergeBeforeGenreSelection() throws Exception {
        var f=fixture(false);
        var features=context.getBean(TrackFeatureRepository.class);
        var rows=features.findAll();
        String[] genres={"hip-hop","hip hop","rock","rock","jazz"};
        for(int i=0;i<rows.size();i++){
            var row=rows.get(i);row.updateMetadataFeatures("balanced",genres[i],row.getEnergy(),row.getValence(),"LASTFM_TAG",null,"{}",LocalDateTime.now());
            features.save(row);
        }
        var json=mapper.readTree(context.getBean(MusicFeatureClient.class).collectFeatures(f.id()));
        assertEquals(2,json.path("genreDistribution").path("HIP_HOP").asInt());
        assertEquals("HIP_HOP",json.path("primaryGenre").asText());
    }

    @Test void corruptV2MetadataDoesNotSilentlyRenameCharacter() {
        var f=fixture(false);
        context.getBean(CharacterRepository.class).save(CharacterEntity.create(
                context.getBean(PlaylistRepository.class).findById(f.id()).orElseThrow(),1,"https://fixture.local/bad.png","prompt","{}","{\"schemaVersion\":2}",null));
        assertThrows(com.playlist.plitter.global.exception.ApiException.class,
                () -> context.getBean(CharacterService.class).getCharacter(f.id(),f.owner()));
    }

    @Test void turningOffNewGenerationKeepsPreviouslyStoredNames() throws Exception {
        var f=fixture(false);
        var service=context.getBean(CharacterService.class);
        var response=service.createCharacter(f.id(),f.owner());
        setV2Enabled(false);
        assertEquals(response.characterName(),service.getCharacter(f.id(),f.owner()).characterName());
        assertEquals(response.tags(),service.getCharacterDownloadUrl(f.id(),f.owner()).tags());
    }

    @Test void generationCanUseLegacyRulesWithoutChangingResponseShape() throws Exception {
        var f=fixture(false);
        setV2Enabled(false);
        var response=context.getBean(CharacterService.class).createCharacter(f.id(),f.owner());
        var saved=context.getBean(CharacterRepository.class).findById(response.characterId()).orElseThrow();
        assertNull(saved.getGenerationSpecJson());
        assertNotNull(saved.getResultMetadataJson());
        assertEquals("발라드 무드 스타",response.characterName());
        assertEquals(response.characterName(),context.getBean(CharacterService.class).getCharacter(f.id(),f.owner()).characterName());
    }

    @Test void ownershipIsCheckedBeforeCreation() {
        var f=fixture(false);
        assertThrows(com.playlist.plitter.global.exception.ApiException.class,
                () -> context.getBean(CharacterService.class).createCharacter(f.id(),f.owner()+100));
        assertEquals(0,context.getBean(CharacterRepository.class).count());
    }

    @Test void recommendationGateIsPreserved() {
        var f=fixture(false);
        var repository=context.getBean(RecommendationsRepository.class);
        repository.delete(repository.findAll().get(0));
        assertThrows(com.playlist.plitter.global.exception.ApiException.class,
                () -> context.getBean(CharacterService.class).createCharacter(f.id(),f.owner()));
        assertEquals(0,context.getBean(CharacterRepository.class).count());
    }

    private void setV2Enabled(boolean enabled) throws Exception {
        Object service=context.getBean(CharacterService.class);
        var source=((org.springframework.aop.framework.Advised)service).getTargetSource().getTarget();
        var field=CharacterService.class.getDeclaredField("v2Enabled");field.setAccessible(true);field.setBoolean(source,enabled);
    }

    private record Fixture(Long id,Long owner) {}
    private Fixture fixture(boolean duplicateTrack) {
        var user=context.getBean(UserRepository.class).save(UserEntity.builder().role(UserEntity.Role.USER).nickname("fixture").build());
        var playlist=context.getBean(PlaylistRepository.class).save(PlaylistEntity.builder().owner(user).shortId(UUID.randomUUID().toString().substring(0,12)).build());
        TrackEntity track=null;
        for(int i=0;i<5;i++) {
            if(!duplicateTrack||track==null) {
                track=context.getBean(TrackRepository.class).save(TrackEntity.builder().spotifyTrackId("fixture-"+i).title("Fixture "+i).artistName("Artist").build());
                context.getBean(TrackFeatureRepository.class).save(TrackFeatureEntity.builder().track(track).energy(new BigDecimal("0.28"))
                        .valence(new BigDecimal("0.30")).genre("ballad").confidence(new BigDecimal("0.70"))
                        .featureSource("LASTFM_TAG").rawFeatureJson("{}").fetchedAt(LocalDateTime.now()).build());
            }
            context.getBean(RecommendationsRepository.class).save(RecommendationsEntity.builder().playlist(playlist).track(track).recommenderUser(user).comment("fixture-"+i).build());
        }
        return new Fixture(playlist.getId(),user.getId());
    }
}
