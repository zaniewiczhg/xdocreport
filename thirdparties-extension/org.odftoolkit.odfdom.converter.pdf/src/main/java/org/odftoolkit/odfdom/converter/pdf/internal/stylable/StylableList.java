/**
 * Copyright (C) 2011 The XDocReport Team <xdocreport@googlegroups.com>
 *
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.odftoolkit.odfdom.converter.pdf.internal.stylable;

import java.util.ArrayList;
import java.util.Map;

import org.odftoolkit.odfdom.converter.pdf.internal.styles.Style;
import org.odftoolkit.odfdom.converter.pdf.internal.styles.StyleListProperties;
import org.odftoolkit.odfdom.converter.pdf.internal.styles.StyleNumFormat;
import org.odftoolkit.odfdom.converter.pdf.internal.styles.StyleTextProperties;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.List;
import com.lowagie.text.ListItem;
import com.lowagie.text.Paragraph;
import com.lowagie.text.factories.RomanAlphabetFactory;
import com.lowagie.text.factories.RomanNumberFactory;

import fr.opensagres.xdocreport.itext.extension.IITextContainer;

/**
 * fixes for pdf conversion by Leszek Piotrowicz <leszekp@safe-mail.net>
 */
public class StylableList
    extends List
    implements IStylableContainer
{
    private IStylableContainer parent;

    private int listLevel;

    private boolean romanNumbered = false;

    private Style lastStyleApplied = null;

    public StylableList( StylableDocument ownerDocument, IStylableContainer parent, int listLevel )
    {
        this.parent = parent;
        this.listLevel = listLevel;
        // set defaults
        super.setNumbered( false );
        super.setLettered( false );
        super.setLowercase( false );
        super.setPreSymbol( "" );
        super.setPostSymbol( "" );
        super.setListSymbol( "" );
        super.setAutoindent( false );
    }

    public int getIndex()
    {
        return first + list.size();
    }

    public void setFirst( int first )
    {
        this.first = first;
    }

    public void addElement( Element element )
    {
        if ( element instanceof StylableListItem )
        {
            StylableListItem li = (StylableListItem) element;
            boolean first = true;
            for ( Element e : li.getElements() )
            {
                addElement( e, first );
                first = false;
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private void addElement( Element element, boolean addLabel )
    {
        if ( element instanceof Paragraph )
        {
            Paragraph p = (Paragraph) element;
            ListItem li = new StylableListItem( p );
            // determine font, it may be set explicitly or use paragraph font
            Font symbolFont = symbol.getFont();
            if ( symbolFont.isStandardFont() )
            {
                ArrayList<Chunk> chunks = p.getChunks();
                for ( Chunk chunk : chunks )
                {
                    // use first specified font
                    if ( !chunk.getFont().isStandardFont() )
                    {
                        symbolFont = chunk.getFont();
                        break;
                    }
                }
                if ( symbolFont.isStandardFont() )
                {
                    // use paragraph font
                    symbolFont = p.getFont();
                }
            }
            if ( addLabel )
            {
                if ( numbered || lettered || romanNumbered )
                {
                    Chunk chunk = new Chunk( preSymbol, symbolFont );
                    int index = first + list.size();
                    if ( lettered )
                    {
                        chunk.append( RomanAlphabetFactory.getString( index, lowercase ) );
                    }
                    else if ( romanNumbered )
                    {
                        chunk.append( RomanNumberFactory.getString( index, lowercase ) );
                    }
                    else
                    {
                        chunk.append( String.valueOf( index ) );
                    }
                    chunk.append( postSymbol );
                    li.setListSymbol( chunk );
                }
                else
                {
                    li.setListSymbol( new Chunk( symbol.getContent(), symbolFont ) );
                }
            }
            else
            {
                li.setListSymbol( new Chunk( "", symbolFont ) );
            }
            li.setIndentationLeft( symbolIndent );
            li.setIndentationRight( 0.0f );
            list.add( li );
        }
        else if ( element instanceof List )
        {
            List l = (List) element;
            // open office specifies absolute list indentation
            // but iText computes indentation relative to parent list
            // so we have to set difference
            l.setIndentationLeft( l.getIndentationLeft() - this.getIndentationLeft() );
            first--;
            list.add( l );
        }
    }

    public void applyStyles( Style style )
    {
        this.lastStyleApplied = style;

        Map<Integer, StyleListProperties> listPropertiesMap = style.getListPropertiesMap();
        if ( listPropertiesMap != null )
        {
            StyleListProperties listProperties = null;
            for ( int i = listLevel; i >= 0 && listProperties == null; i-- )
            {
                // find style for current or nearest lower list level
                listProperties = listPropertiesMap.get( i );
            }
            if ( listProperties != null )
            {
                String bulletChar = listProperties.getBulletChar();
                if ( bulletChar != null )
                {
                    // list item label is a char
                    Chunk symbol = new Chunk( bulletChar );

                    if ( listProperties.isLabelStyleSpecified() )
                    {
                        StyleTextProperties textProperties = style.getTextProperties();
                        if ( textProperties != null )
                        {
                            Font font = textProperties.getFont();
                            if ( font != null )
                            {
                                symbol.setFont( font );
                            }
                        }
                    }

                    super.setListSymbol( symbol );
                }

                Image image = listProperties.getImage();
                if ( image != null )
                {
                    // list item label is an image
                    Float width = listProperties.getWidth();
                    if ( width != null )
                    {
                        image.scaleAbsoluteWidth( width );
                    }

                    Float height = listProperties.getHeight();
                    if ( height != null )
                    {
                        image.scaleAbsoluteHeight( height );
                    }

                    super.setListSymbol( new Chunk( image, 0.0f, 0.0f ) );
                }

                if ( bulletChar == null && image == null )
                {
                    // list item label is a number
                    Chunk symbol = new Chunk( "" );

                    if ( listProperties.isLabelStyleSpecified() )
                    {
                        StyleTextProperties textProperties = style.getTextProperties();
                        if ( textProperties != null )
                        {
                            Font font = textProperties.getFont();
                            if ( font != null )
                            {
                                symbol.setFont( font );
                            }
                        }
                    }

                    Integer startValue = listProperties.getStartValue();
                    if ( startValue != null )
                    {
                        super.setFirst( startValue );
                    }

                    String numPrefix = listProperties.getNumPrefix();
                    if ( numPrefix != null )
                    {
                        super.setPreSymbol( numPrefix );
                        symbol = new Chunk( numPrefix, symbol.getFont() );
                    }

                    String numSuffix = listProperties.getNumSuffix();
                    if ( numSuffix != null )
                    {
                        super.setPostSymbol( numSuffix );
                        symbol.append( numSuffix );
                    }

                    StyleNumFormat numFormat = listProperties.getNumFormat();
                    if ( numFormat != null )
                    {
                        super.setNumbered( true );
                        super.setLettered( numFormat.isAlphabetical() );
                        this.romanNumbered = numFormat.isRoman();
                        super.setLowercase( numFormat.isLowercase() );
                    }

                    super.setListSymbol( symbol );
                }

                // set indentation, it is specified in different way by Open Office and MsWord
                Float marginLeft = listProperties.getMarginLeft();
                Float textIndent = listProperties.getTextIndent();
                Float spaceBefore = listProperties.getSpaceBefore();
                Float minLabelWidth = listProperties.getMinLabelWidth();
                if ( marginLeft != null && textIndent != null )
                {
                    // ODT generated by Open Office
                    super.setIndentationLeft( Math.max( marginLeft + textIndent, 0.0f ) );
                    super.setSymbolIndent( Math.max( -textIndent, 0.0f ) );
                }
                else if ( spaceBefore != null && minLabelWidth != null )
                {
                    // ODT generated by MsWord
                    super.setIndentationLeft( Math.max( spaceBefore - minLabelWidth, 0.0f ) );
                    super.setSymbolIndent( Math.max( minLabelWidth, 0.0f ) );
                }
            }
        }
    }

    public Style getLastStyleApplied()
    {
        return lastStyleApplied;
    }

    public IStylableContainer getParent()
    {
        return parent;
    }

    public Element getElement()
    {
        return this;
    }

    public IITextContainer getITextContainer()
    {
        return parent;
    }

    public void setITextContainer( IITextContainer container )
    {
    }
}
