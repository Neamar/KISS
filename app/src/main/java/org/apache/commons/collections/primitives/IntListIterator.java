/*
 * $Header: /home/cvs/jakarta-commons/primitives/src/java/org/apache/commons/collections/primitives/IntListIterator.java,v 1.3 2003/10/16 20:49:36 scolebourne Exp $
 * ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.commons.collections.primitives;

/**
 * A bi-directional iterator over <code>int</code> values.
 *
 * @see org.apache.commons.collections.primitives.adapters.IntListIteratorListIterator
 * @see org.apache.commons.collections.primitives.adapters.ListIteratorIntListIterator
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 1.3 $ $Date: 2003/10/16 20:49:36 $
 * 
 * @author Rodney Waldhoff 
 */
public interface IntListIterator extends IntIterator {
    /**
     * Inserts the specified element into my underlying collection
     * (optional operation).
     * The element is inserted immediately before the next element 
     * that would have been returned by {@link #next}, if any,
     * and immediately after the next element that would have been 
     * returned by {@link #previous}, if any.
     * <p/>
     * The new element is inserted immediately before the implied
     * cursor. A subsequent call to {@link #previous} will return
     * the added element, a subsequent call to {@link #next} will
     * be unaffected.  This call increases by one the value that
     * would be returned by a call to {@link #nextIndex} or 
     * {@link #previousIndex}.
     * 
     * @param element the value to be inserted
     * 
     * @throws UnsupportedOperationException when this operation is not 
     *         supported
     * @throws IllegalArgumentException if some aspect of the specified element 
     *         prevents it from being added
     */
    void add(int element);

    /** 
     * Returns <code>true</code> iff I have more elements
     * when traversed in the forward direction. 
     * (In other words, returns <code>true</code> iff 
     * a call to {@link #next} will return an element
     * rather than throwing an exception.
     * 
     * @return <code>true</code> iff I have more elements when 
     *         traversed in the forward direction
     */
    boolean hasNext();
    
    /** 
     * Returns <code>true</code> iff I have more elements
     * when traversed in the reverse direction. 
     * (In other words, returns <code>true</code> iff 
     * a call to {@link #previous} will return an element
     * rather than throwing an exception.
     * 
     * @return <code>true</code> iff I have more elements when 
     *         traversed in the reverse direction
     */
    boolean hasPrevious();

    /** 
     * Returns the next element in me when traversed in the
     * forward direction.
     * 
     * @return the next element in me
     * @throws NoSuchElementException if there is no next element
     */          
    int next();
    
    /** 
     * Returns the index of the element that would be returned
     * by a subsequent call to {@link #next}, or the number 
     * of elements in my iteration if I have no next element.
     * 
     * @return the index of the next element in me
     */          
    int nextIndex();

    /** 
     * Returns the next element in me when traversed in the
     * reverse direction.
     * 
     * @return the previous element in me
     * @throws NoSuchElementException if there is no previous element
     */          
    int previous();

    /** 
     * Returns the index of the element that would be returned
     * by a subsequent call to {@link #previous}, or 
     * <code>-1</code> if I have no previous element.
     * 
     * @return the index of the previous element in me
     */          
    int previousIndex();

    /** 
     * Removes from my underlying collection the last 
     * element returned by {@link #next} or {@link #previous}
     * (optional operation). 
     * 
     * @throws UnsupportedOperationException if this operation is not 
     *         supported
     * @throws IllegalStateException if neither {@link #next} nor
     *         {@link #previous} has yet been called, or 
     *         {@link #remove} or {@link #add} has already been called since 
     *         the last call to {@link #next} or {@link #previous}.
     */          
    void remove();

    /** 
     * Replaces in my underlying collection the last 
     * element returned by {@link #next} or {@link #previous}
     * with the specified value (optional operation). 
     * 
     * @param element the value to replace the last returned element with
     * @throws UnsupportedOperationException if this operation is not 
     *         supported
     * @throws IllegalStateException if neither {@link #next} nor
     *         {@link #previous} has yet been called, or 
     *         {@link #remove} or {@link #add} has already been called since 
     *         the last call to {@link #next} or {@link #previous}.
     * @throws IllegalArgumentException if some aspect of the specified element 
     *         prevents it from being added
     */          
    void set(int element);
}
