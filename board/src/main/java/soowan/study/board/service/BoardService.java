package soowan.study.board.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soowan.study.board.dto.BoardRequestDTO;
import soowan.study.board.dto.BoardResponseDTO;
import soowan.study.board.dto.SuccessResponseDTO;
import soowan.study.board.entity.Board;
import soowan.study.board.exception.InvalidPasswordException;
import soowan.study.board.repository.BoardRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    @Transactional(readOnly = true)
    public List<BoardResponseDTO> getPosts() {
        return boardRepository.findAllByOrderByModifiedAtDesc()
                .stream()
                .map(BoardResponseDTO::new)
                .toList();
    }

    @Transactional
    public BoardResponseDTO createPost(BoardRequestDTO boardRequestDTO) {
        Board board = new Board(boardRequestDTO);
        boardRepository.save(board);
        return new BoardResponseDTO(board);
    }

    @Transactional
    public BoardResponseDTO getPost(Long id) {
        return boardRepository.findById(id).map(BoardResponseDTO::new)
                .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));
    }

    @Transactional
    public BoardResponseDTO updatePost(Long id, BoardRequestDTO boardRequestDTO) throws Exception {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));

        if (!boardRequestDTO.getPassword().equals(board.getPassword())) {
            throw new InvalidPasswordException();
        }

        board.update(boardRequestDTO);
        return new BoardResponseDTO(board);
    }

    @Transactional
    public SuccessResponseDTO deletePost(Long id, BoardRequestDTO boardRequestDTO) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));

        if (!boardRequestDTO.getPassword().equals(board.getPassword())) {
            throw new InvalidPasswordException();
        }

        boardRepository.deleteById(id);
        return new SuccessResponseDTO(true);
    }
}
