package com.example.bookshelf.controller;

import com.example.bookshelf.storage.BookStorage;
import com.example.bookshelf.storage.impl.StaticListBookStorageImpl;
import com.example.bookshelf.type.Book;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static fi.iki.elonen.NanoHTTPD.Response.Status.*;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;


public class BookController {

    private BookStorage bookStorage = new StaticListBookStorageImpl();

    public NanoHTTPD.Response serveGetBookRequest(NanoHTTPD.IHTTPSession session) {
        Map<String,List<String>> requestParams = session.getParameters();
        if(requestParams.containsKey("id")) {
            List<String> bookIdParams = requestParams.get("id");
            String bookIdParam = bookIdParams.get(0);
            long bookId = 0;

            try {
                bookId = Long.parseLong(bookIdParam);
            } catch (NumberFormatException nfm) {
                System.err.println("Error during process request: \n"+nfm);
                return newFixedLengthResponse(BAD_REQUEST,"text/plain","BookId must be a number!");
            }

            Book book = bookStorage.getBook(bookId);

            if(book != null){
                try{
                    ObjectMapper objectMapper = new ObjectMapper();
                    String response = objectMapper.writeValueAsString(book);
                    return newFixedLengthResponse(OK,"application/json",response);
                } catch (JsonProcessingException e) {
                    System.err.println("Error during process request: \n"+e);
                    return newFixedLengthResponse(INTERNAL_ERROR,"text/plain","Internal error can't read all the books");
                }
            }

            return newFixedLengthResponse(NOT_FOUND,"application/json","");
        }


        return newFixedLengthResponse(NOT_FOUND,"text/plain","Uncorrected request params");
    }

    public NanoHTTPD.Response serveGetBooksRequest(NanoHTTPD.IHTTPSession session) {
        ObjectMapper objectMapper = new ObjectMapper();
        String response = "";

        try {
            response=objectMapper.writeValueAsString(bookStorage.getAllBooks());
        } catch (JsonProcessingException e) {
            System.err.println("Error during process request: \n"+e);
            return newFixedLengthResponse(INTERNAL_ERROR,"text/plain","Internal error can't read all the books");
        }
        return newFixedLengthResponse(OK,"application/json",response);
    }

    public NanoHTTPD.Response serveAddBookRequest(NanoHTTPD.IHTTPSession session) {
        ObjectMapper objectMapper = new ObjectMapper();

        long randomBookId = System.currentTimeMillis();

        String lengthHeader = session.getHeaders().get("content-length");

        int contentLength = Integer.parseInt(lengthHeader);

        byte[] buffer = new byte[contentLength];

        try{
            session.getInputStream().read(buffer,0,contentLength);
            String requestBody = new String(buffer).trim();
            Book requestBook = objectMapper.readValue(requestBody,Book.class);
            requestBook.setId(randomBookId);

            bookStorage.addBook(requestBook);
        } catch (IOException e) {
            System.err.println("Error during process request: \n"+e);
            return newFixedLengthResponse(INTERNAL_ERROR,"text/plain","Can't add");
        }

        return newFixedLengthResponse(OK,"application/json","Book added,id= " + randomBookId);
    }
}
