package soowan.study.board.dto;

import lombok.Getter;

@Getter
public class BoardRequestDTO {
    private String title;
    private String content;
    private String author;
    private String password;
}
