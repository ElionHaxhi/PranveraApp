package com.pranveraapp.core.web.controller.news.support;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by elion on 11/02/16.
 */
public class PageElementsBuilder {

    private final long currentPage;
    private final long totalPages;
    private long startPage;
    private long endPage;

    PageElementsBuilder(long currentPage,long totalPages){
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }

    public List<PageElement> build(){
        ArrayList<PageElement> elements = new ArrayList<>();

        findStartPage();
        findEndPage();

        if(startPage > 1){
            //addFirstPageElements
            addFirstPage(elements);
        }

        addPageElements(elements);

        if(endPage < totalPages){
            //addLastPage
            addLastPage(elements);
        }

        return elements;
    }

    private void findStartPage(){
        long previousTwoPages = Math.max((currentPage - 2), 1L);
        startPage = previousTwoPages;
    }

    private void findEndPage(){
        long nextThreePages = currentPage + 3;
        long desiredLastPage = Math.max(6, nextThreePages);
        endPage = Math.min(desiredLastPage,totalPages);
    }

    private void addPageElements(ArrayList<PageElement> elements){
       for (long n = startPage; n <= endPage; n++) {
            boolean isCurrentPage = n == currentPage;
            boolean isNavigable = !isCurrentPage;
            elements.add(new PageElement(n, isNavigable, isCurrentPage));
        }
    }

    private void addFirstPage(ArrayList<PageElement> elements){
        elements.add(new PageElement(1,true,false));
        if(startPage > 2){
            addEllipsis(elements);
        }
    }

    private void addLastPage(ArrayList<PageElement> elements){
        if(endPage < totalPages -1){
            addEllipsis(elements);
        }
        elements.add(new PageElement(totalPages,true,false));
    }

    private void addEllipsis(ArrayList<PageElement> elements){
        elements.add(new PageElement("...",false,false));
    }






}
