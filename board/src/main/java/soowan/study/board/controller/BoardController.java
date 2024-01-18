package soowan.study.board.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import soowan.study.board.dto.BoardRequestDTO;
import soowan.study.board.dto.BoardResponseDTO;
import soowan.study.board.dto.SuccessResponseDTO;
import soowan.study.board.service.BoardService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/posts")
    public List<BoardResponseDTO> getPosts() {
        return boardService.getPosts();
    }

    @PostMapping("/post")
    public BoardResponseDTO createPost(@RequestBody BoardRequestDTO boardRequestDTO) {
        return boardService.createPost(boardRequestDTO);
    }

    @GetMapping("/post/{id}")
    public BoardResponseDTO getPost(@PathVariable Long id) {
        return boardService.getPost(id);
    }

    @PutMapping("/post/{id}")
    public BoardResponseDTO updatePost(@PathVariable Long id, @RequestBody BoardRequestDTO boardRequestDTO) throws Exception {
        return boardService.updatePost(id, boardRequestDTO);
    }

    @DeleteMapping("/post/{id}")
    public SuccessResponseDTO deletePost(@PathVariable Long id, @RequestBody BoardRequestDTO boardRequestDTO) {
        return boardService.deletePost(id, boardRequestDTO);
    }
}
