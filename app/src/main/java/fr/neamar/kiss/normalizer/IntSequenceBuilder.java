package fr.neamar.kiss.normalizer;

/**
 * Simple integer sequence class that allows adding individual elements and exporting those
 * elements to an integer array.
 * <p/>
 * Created by Alexander Schlarb on 17.08.15.
 */
class IntSequenceBuilder {
    private int[] data;
    private int size;


    /**
     * @param capacity The initial size of the internal storage array
     */
    public IntSequenceBuilder(int capacity) {
        // Create new storage array of requested size
        this.data = new int[capacity];
        this.size = 0;
    }


    /**
     * Add a new element to this builder
     *
     * @param element The value of the element to add
     */
    public void add(int element) {
        // Resize storage array larger if required
        if ((this.size + 1) >= this.data.length) {
            int[] data = this.data;
            this.data = new int[(this.data.length * 3) / 2 + 1];
            System.arraycopy(data, 0, this.data, 0, this.size);
        }

        // Add element to storage array
        this.data[this.size] = element;

        // Increment stored element number counter
        this.size++;
    }


    /**
     * Export an array with the current data stored in this builder
     *
     * @return Copy of the elements of the internal storage array
     */
    public int[] toArray() {
        // Copy the actual number of stored elements to a new array
        int[] data = new int[this.size];
        System.arraycopy(this.data, 0, data, 0, this.size);

        return data;
    }
}
