package com.remoteclassroom.backend.service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.remoteclassroom.backend.model.Video;
import com.remoteclassroom.backend.repository.VideoRepository;

@Service
public class TranscriptionService {

    @Autowired
    private WhisperService whisperService;

    @Autowired
    private VideoRepository videoRepository;

    // 🔥 ADD
    @Autowired
    private QuizService quizService;

    @Async
    public void transcribeVideoAsync(Long videoId) {

        File videoFile = null;
        File audioFile = null;

        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            System.out.println("🔥 Processing video: " + videoId);

            videoFile = downloadFromUrl(video.getUrl());
            audioFile = convertToMp3(videoFile);

            String transcript = whisperService.transcribe(audioFile);

            video.setTranscript(transcript);
            videoRepository.save(video);

            // 🔥 FIX: Generate quiz ONLY if not exists
            boolean quizExists = quizService.getQuizByVideo(video.getId()).isPresent();

            if (!quizExists) {
                System.out.println("🔥 Generating quiz for video: " + videoId);
                quizService.generateAndSaveQuiz(video.getId());
            } else {
                System.out.println("🔥 Quiz already exists for video: " + videoId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (videoFile != null) videoFile.delete();
            if (audioFile != null) audioFile.delete();
        }
    }

    private File downloadFromUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        File file = File.createTempFile("video_", ".tmp");

        try (InputStream in = url.openStream()) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return file;
    }

    private File convertToMp3(File videoFile) throws Exception {

        File audioFile = File.createTempFile("audio_", ".mp3");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", videoFile.getAbsolutePath(),
                "-vn",
                "-ac", "1",
                "-ar", "16000",
                "-b:a", "32k",
                audioFile.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();

        return audioFile;
    }
}