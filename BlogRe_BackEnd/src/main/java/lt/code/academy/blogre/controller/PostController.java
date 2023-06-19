package lt.code.academy.blogre.controller;

import com.fasterxml.jackson.databind.JsonNode;

import lt.code.academy.blogre.dto.ContentRequest;
import lt.code.academy.blogre.entity.Post;
import lt.code.academy.blogre.repository.BlogUserRepository;
import lt.code.academy.blogre.repository.PostRepository;
import lt.code.academy.blogre.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PostController {
    private final PostRepository postRepository;
    private final AuthService authService;

    public PostController(
            PostRepository postRepository,
            BlogUserRepository blogUserRepository,
            AuthService authService
    ) {
        this.postRepository = postRepository;
        this.authService = authService;
    }

    @GetMapping(value = "/posts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<JsonNode>> posts() {
        return ResponseEntity.ok(
                postRepository.findAll().stream()
                        .map(Post::asJson)
                        .toList()
        );
    }

    @GetMapping(value = "/posts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> post(@PathVariable Long id) {
        return postRepository.findById(id)
                .map(Post::asJsonWithComments)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/posts")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<JsonNode> createPost(@RequestBody ContentRequest contentRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postRepository.save(newPost(contentRequest)).asJson());
    }

    private Post newPost(ContentRequest contentRequest) {
        return new Post(
                contentRequest.getTitle(),
                contentRequest.getContent(),
                LocalDate.now(),
                LocalDate.now(),
                authService.getBlogUser().orElseThrow(),
                new ArrayList<>()
        );
    }

    @PutMapping(value = "/posts")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<JsonNode> updatePost(@RequestBody ContentRequest contentRequest) {
        return postRepository.findById(contentRequest.getId())
                .map(post -> post.updateContent(contentRequest.getContent()))
                .map(post -> ResponseEntity.ok(postRepository.save(post).asJson()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/posts/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<HttpStatus> deletePost(@PathVariable Long id) {
        postRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
