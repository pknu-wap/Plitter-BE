-- 1) 점검: unknown/null 패턴 feature 확인
select
  tf.id,
  tf.track_id,
  t.title,
  t.artist_name,
  tf.genre,
  tf.mood,
  tf.energy,
  tf.valence,
  tf.feature_source,
  tf.confidence,
  tf.fetched_at
from track_features tf
join tracks t on t.id = tf.track_id
where tf.feature_source is null
   or (tf.genre = 'unknown' and tf.mood is null and tf.energy is null and tf.valence is null)
order by tf.id desc;

-- 2) 정리(필요 시): 위 패턴만 삭제
-- delete from track_features tf
-- where tf.feature_source is null
--    or (tf.genre = 'unknown' and tf.mood is null and tf.energy is null and tf.valence is null);
