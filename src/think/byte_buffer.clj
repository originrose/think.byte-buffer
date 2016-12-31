(ns think.byte-buffer
  (:import [think.byte_buffer ByteBuffer
            ByteBuffer$EndianType
            ByteBuffer$Datatype
            ByteBuffer$BufferManager]))


(defn create-manager
  []
  (ByteBuffer$BufferManager/create_buffer_manager))
