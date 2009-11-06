/*
 * Copyright (c) 2002-2009, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.portal.service.search;

import fr.paris.lutece.portal.business.page.Page;
import fr.paris.lutece.portal.business.page.PageHome;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.page.PageService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.url.UrlItem;

import org.apache.lucene.demo.html.HTMLParser;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;


/**
 * Indexer service for pages
 */
public class PageIndexer implements SearchIndexer
{
    public static final String INDEXER_NAME = "PageIndexer";
    private static final String INDEXER_DESCRIPTION = "Indexer service for pages";
    private static final String INDEXER_VERSION = "1.0.0";
    private static final String PROPERTY_PAGE_BASE_URL = "search.pageIndexer.baseUrl";
    private static final String PROPERTY_INDEXER_ENABLE = "search.pageIndexer.enable";
    private static final String PARAMETER_PAGE_ID = "page_id";

    /**
     * {@inheritDoc}
     */
    public void indexDocuments(  ) throws IOException, InterruptedException, SiteMessageException
    {
        String strPageBaseUrl = AppPropertiesService.getProperty( PROPERTY_PAGE_BASE_URL );
        List<Page> listPages = PageHome.getAllPages(  );

        for ( Page page : listPages )
        {
            UrlItem url = new UrlItem( strPageBaseUrl );
            url.addParameter( PARAMETER_PAGE_ID, page.getId(  ) );

            Document doc = getDocument( page, url.getUrl(  ) );
            IndexationService.write( doc );
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Document> getDocuments( String nIdDocument )
        throws IOException, InterruptedException, SiteMessageException
    {
        ArrayList<Document> listDocuments = new ArrayList<Document>(  );
        String strPageBaseUrl = AppPropertiesService.getProperty( PROPERTY_PAGE_BASE_URL );

        Page page = PageHome.getPage( Integer.parseInt( nIdDocument ) );

        if ( ( page != null ) && ( page.getId(  ) != 0 ) )
        {
            UrlItem url = new UrlItem( strPageBaseUrl );
            url.addParameter( PARAMETER_PAGE_ID, page.getId(  ) );

            Document doc = getDocument( page, url.getUrl(  ) );
            listDocuments.add( doc );
        }

        return listDocuments;
    }

    /**
     * {@inheritDoc}
     */
    public String getName(  )
    {
        return INDEXER_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public String getVersion(  )
    {
        return INDEXER_VERSION;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription(  )
    {
        return INDEXER_DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnable(  )
    {
        String strEnable = AppPropertiesService.getProperty( PROPERTY_INDEXER_ENABLE, "true" );

        return ( strEnable.equalsIgnoreCase( "true" ) );
    }

    /**
     * Builds a document which will be used by Lucene during the indexing of the pages of the site with the following
     * fields : summary, uid, url, contents, title and description.
     * @return the built Document
     * @param strUrl The base URL for documents
     * @param page the page to index
     * @throws IOException The IO Exception
     * @throws InterruptedException The InterruptedException
     * @throws SiteMessageException occurs when a site message need to be displayed
     */
    private Document getDocument( Page page, String strUrl )
        throws IOException, InterruptedException, SiteMessageException
    {
        // make a new, empty document
        Document doc = new Document(  );

        // Add the url as a field named "url".  Use an UnIndexed field, so
        // that the url is just stored with the document, but is not searchable.
        doc.add( new Field( SearchItem.FIELD_URL, strUrl, Field.Store.YES, Field.Index.UN_TOKENIZED ) );

        // Add the last modified date of the file a field named "modified".
        // Use a field that is indexed (i.e. searchable), but don't tokenize
        // the field into words.
        String strDate = DateTools.dateToString( page.getDateUpdate(  ), DateTools.Resolution.DAY );
        doc.add( new Field( SearchItem.FIELD_DATE, strDate, Field.Store.YES, Field.Index.UN_TOKENIZED ) );

        // Add the uid as a field, so that index can be incrementally maintained.
        // This field is not stored with document, it is indexed, but it is not
        // tokenized prior to indexing.
        String strIdPage = String.valueOf( page.getId(  ) );
        doc.add( new Field( SearchItem.FIELD_UID, strIdPage, Field.Store.NO, Field.Index.UN_TOKENIZED ) );

        String strPageContent = PageService.getInstance(  ).getPageContent( page.getId(  ), 0, null );
        StringReader readerPage = new StringReader( strPageContent );
        HTMLParser parser = new HTMLParser( readerPage );

        //the content of the article is recovered in the parser because this one
        //had replaced the encoded caracters (as &eacute;) by the corresponding special caracter (as ?)
        Reader reader = parser.getReader(  );
        int c;
        StringBuffer sb = new StringBuffer(  );

        while ( ( c = reader.read(  ) ) != -1 )
        {
            sb.append( String.valueOf( (char) c ) );
        }

        reader.close(  );

        // Add the tag-stripped contents as a Reader-valued Text field so it will
        // get tokenized and indexed.        
        StringBuffer sbFieldContent = new StringBuffer(  );
        sbFieldContent.append( page.getName(  ) + " " + sb.toString(  ) );

        // Add the metadata description of the page if it exists
        if ( page.getMetaDescription(  ) != null )
        {
            sbFieldContent.append( " " + page.getMetaDescription(  ) );
        }

        // Add the metadata keywords of the page if it exists
        if ( page.getMetaKeywords(  ) != null )
        {
            sbFieldContent.append( " " + page.getMetaKeywords(  ) );
        }

        doc.add( new Field( SearchItem.FIELD_CONTENTS, sbFieldContent.toString(  ), Field.Store.NO,
                Field.Index.TOKENIZED ) );

        // Add the title as a separate Text field, so that it can be searched
        // separately.
        doc.add( new Field( SearchItem.FIELD_TITLE, page.getName(  ), Field.Store.YES, Field.Index.NO ) );

        if ( ( page.getDescription(  ) != null ) && ( page.getDescription(  ).length(  ) > 1 ) )
        {
            // Add the summary as an UnIndexed field, so that it is stored and returned
            // with hit documents for display.
            doc.add( new Field( SearchItem.FIELD_SUMMARY, page.getDescription(  ), Field.Store.YES, Field.Index.NO ) );
        }

        doc.add( new Field( SearchItem.FIELD_TYPE, "Page", Field.Store.YES, Field.Index.UN_TOKENIZED ) );
        doc.add( new Field( SearchItem.FIELD_ROLE, page.getRole(  ), Field.Store.YES, Field.Index.UN_TOKENIZED ) );

        // return the document
        return doc;
    }
}
