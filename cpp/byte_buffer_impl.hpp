#ifndef BYTE_BUFFER_IMPL_HPP
#define BYTE_BUFFER_IMPL_HPP
#include <memory>
#include <stdexcept>
#include <cstring>
#include "byte_buffer.hpp"

namespace think { namespace byte_buffer {
    using namespace std;

    template<Datatype::Enum>
    struct datatype_to_type
    {
    };

    template<typename type>
    struct type_to_datatype
    {
    };


#define DEFINE_DATATYPE(dtype,type) \
    template<> struct datatype_to_type<Datatype::dtype> { typedef type TType; }; \
    template<> struct type_to_datatype<type> { static Datatype::Enum datatype() { return Datatype::dtype; } };

    DEFINE_DATATYPE(Byte,uint8_t);
    DEFINE_DATATYPE(Short,int16_t);
    DEFINE_DATATYPE(Int,int32_t);
    DEFINE_DATATYPE(Long,int64_t);
    DEFINE_DATATYPE(Float,float);
    DEFINE_DATATYPE(Double,double);


    template<typename lhs, typename rhs>
    struct copy_op
    {
      static inline void copy(const lhs* src, int64_t src_offset,
			      rhs* dst, int64_t dst_offset,
			      int64_t n_elems)
      {
	dst += dst_offset;
	src += src_offset;
	for(int64_t idx = 0; idx < n_elems; ++idx) {
	  dst[idx] = (rhs) src[idx];
	}
      }
    };

    template<typename dtype>
    struct single_copy_op
    {
      static inline void copy( const dtype* src, int64_t src_offset,
			       dtype* dst, int64_t dst_offset,
			       int64_t n_elems )
      {
	memcpy((char*) (dst + dst_offset),
	       (const char*) (src + src_offset),
	       n_elems * sizeof(dtype));
      }
    };

#define DEFINE_SINGLE_TYPE_COPY(dtype) \
    template<> struct copy_op<dtype,dtype> { \
      static inline void copy(const dtype* src, int64_t src_offset, \
			      dtype * dst, int64_t dst_offset, \
			      int64_t n_elems) { \
	single_copy_op<dtype>::copy( src, src_offset, dst, dst_offset, n_elems);}};


    DEFINE_SINGLE_TYPE_COPY(uint8_t);
    DEFINE_SINGLE_TYPE_COPY(int16_t);
    DEFINE_SINGLE_TYPE_COPY(int32_t);
    DEFINE_SINGLE_TYPE_COPY(int64_t);
    DEFINE_SINGLE_TYPE_COPY(float);
    DEFINE_SINGLE_TYPE_COPY(double);


    template<typename src_type, typename dst_type>
    inline void do_copy(const src_type* src, int64_t src_offset,
			dst_type* dst, int64_t dst_offset,
			int64_t n_elems){
      copy_op<src_type,dst_type>::copy(src, src_offset, dst, dst_offset, n_elems);
    }


    template<typename val_type, typename buf_type>
    struct buf_get {
      static inline val_type get(const buf_type* src, int64_t src_offset ) {
	return static_cast<val_type>(src[src_offset]);
      }
    };

    template<typename val_type, typename buf_type>
    inline val_type do_get( const buf_type* src, int64_t src_offset ) {
      return buf_get<val_type,buf_type>::get(src, src_offset);
    }

    template<typename dst_type, typename val_type>
    struct buf_set {
      static inline void set(dst_type* dst, int64_t dst_offset, val_type value, int64_t n_elems ) {
	dst += dst_offset;
	switch(n_elems) {
	case 1:
	  dst[0] = (dst_type)value;
	  break;
	default:
	  if (0 == value) {
	    memset(dst, 0, n_elems * sizeof(dst_type));
	  }
	  else {
	    for( int64_t idx = 0; idx < n_elems; ++idx )
	      dst[idx] = (dst_type)value;
	  }
	}
      }
    };

    template<>
    struct buf_set<char,char> {
      static inline void set(char* dst, int64_t dst_offset, char value, int64_t n_elems ) {
	dst += dst_offset;
	switch(n_elems) {
	case 1:
	  dst[0] = value;
	  break;
	default:
	  memset(dst, value, n_elems);
	}
      }
    };

    template<typename dst_type, typename val_type>
    inline void do_set(dst_type* dst, int64_t dst_offset, val_type value, int64_t n_elems) {
      buf_set<dst_type,val_type>::set(dst, dst_offset, value, n_elems);
    }

    template<typename TRetType, typename TOpType>
    inline TRetType typed_buffer_op(int64_t data, Datatype::Enum type, TOpType op)
    {
      switch(type) {
      case Datatype::Byte: return op((typename datatype_to_type<Datatype::Byte>::TType*)data);
      case Datatype::Short: return op((typename datatype_to_type<Datatype::Short>::TType*)data);
      case Datatype::Int: return op((typename datatype_to_type<Datatype::Int>::TType*)data);
      case Datatype::Long: return op((typename datatype_to_type<Datatype::Long>::TType*)data);
      case Datatype::Float: return op((typename datatype_to_type<Datatype::Float>::TType*)data);
      case Datatype::Double: return op((typename datatype_to_type<Datatype::Double>::TType*)data);
      };
      throw exception();
      return TRetType();
    }

    struct BufferManagerImpl : public BufferManager
    {
      BufferManagerImpl(){}
      virtual ~BufferManagerImpl(){}
      virtual int64_t allocate_buffer( int64_t size, const char* file, int line )
      {
	return reinterpret_cast<int64_t>(malloc(size));
      }
      virtual void release_buffer( int64_t data)
      {
	free((void*)data);
      }

      template<typename dst_type>
      void buffer_to_data( int64_t src_data, Datatype::Enum src_type, int64_t src_offset,
		 dst_type* dst, int64_t dst_offset, int64_t n_elems )
      {
	typed_buffer_op<void>(src_data, src_type,
			      [=](auto src_ptr) {
				do_copy(src_ptr, src_offset, dst, dst_offset, n_elems);
			      });
      }


      virtual void copy( int64_t src_data, Datatype::Enum src_type, int64_t src_offset,
			 uint8_t* dst, int64_t dst_offset, int64_t n_elems ) {
	buffer_to_data( src_data, src_type, src_offset, dst, dst_offset, n_elems );
      }
      virtual void copy( int64_t src_data, Datatype::Enum src_type, int64_t src_offset,
			 int16_t* dst, int64_t dst_offset, int64_t n_elems ) {
	buffer_to_data( src_data, src_type, src_offset, dst, dst_offset, n_elems );
      }
      virtual void copy( int64_t src_data, Datatype::Enum src_type, int64_t src_offset,
			 int32_t* dst, int64_t dst_offset, int64_t n_elems ) {
	buffer_to_data( src_data, src_type, src_offset, dst, dst_offset, n_elems );
      }
      virtual void copy( int64_t src_data, Datatype::Enum src_type, int64_t src_offset,
			 int64_t* dst, int64_t dst_offset, int64_t n_elems ) {
	buffer_to_data( src_data, src_type, src_offset, dst, dst_offset, n_elems );
      }
      virtual void copy( int64_t src_data, Datatype::Enum src_type, int64_t src_offset,
			 float* dst, int64_t dst_offset, int64_t n_elems ) {
	buffer_to_data( src_data, src_type, src_offset, dst, dst_offset, n_elems );
      }
      virtual void copy( int64_t src_data, Datatype::Enum src_type, int64_t src_offset,
			 double* dst, int64_t dst_offset, int64_t n_elems ) {
	buffer_to_data( src_data, src_type, src_offset, dst, dst_offset, n_elems );
      }


      template<typename src_type>
      void data_to_buffer( const src_type* src, int64_t src_offset,
			   int64_t dst_data, Datatype::Enum dst_type, int64_t dst_offset,
			   int64_t n_elems )
      {
	typed_buffer_op<void>(dst_data, dst_type,
			      [=](auto dst_ptr) {
				do_copy(src, src_offset, dst_ptr, dst_offset, n_elems);
			      });
      }

      virtual void copy( const uint8_t* src, int64_t src_offset,
			 int64_t dst_data, Datatype::Enum dst_type, int64_t dst_offset, int64_t n_elems ) {
	data_to_buffer(src, src_offset, dst_data, dst_type, dst_offset, n_elems );
      }
      virtual void copy( const int16_t* src, int64_t src_offset,
			 int64_t dst_data, Datatype::Enum dst_type, int64_t dst_offset, int64_t n_elems ) {
	data_to_buffer(src, src_offset, dst_data, dst_type, dst_offset, n_elems );
      }
      virtual void copy( const int32_t* src, int64_t src_offset,
			 int64_t dst_data, Datatype::Enum dst_type, int64_t dst_offset, int64_t n_elems ) {
	data_to_buffer(src, src_offset, dst_data, dst_type, dst_offset, n_elems );
      }
      virtual void copy( const int64_t* src, int64_t src_offset,
			 int64_t dst_data, Datatype::Enum dst_type, int64_t dst_offset, int64_t n_elems ) {
	data_to_buffer(src, src_offset, dst_data, dst_type, dst_offset, n_elems );
      }
      virtual void copy( const float* src, int64_t src_offset,
			 int64_t dst_data, Datatype::Enum dst_type, int64_t dst_offset, int64_t n_elems ) {
	data_to_buffer(src, src_offset, dst_data, dst_type, dst_offset, n_elems );
      }
      virtual void copy( const double* src, int64_t src_offset,
			 int64_t dst_data, Datatype::Enum dst_type, int64_t dst_offset, int64_t n_elems ) {
	data_to_buffer(src, src_offset, dst_data, dst_type, dst_offset, n_elems );
      }


      virtual void copy( int64_t src_data, Datatype::Enum src_type, int64_t src_offset,
			 int64_t dst_data, Datatype::Enum dst_type, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(src_data, src_type, [=](auto src_ptr) {
	    typed_buffer_op<void>(dst_data, dst_type, [=](auto dst_ptr) {
		do_copy(src_ptr, src_offset,
			dst_ptr, dst_offset, n_elems);
	      } );
	  } );
      }


      template<typename src_type>
      void set_buffer_value( int64_t dst_data, Datatype::Enum dst_type, int64_t offset, src_type value, int64_t n_elems ) {
	typed_buffer_op<void>(dst_data, dst_type,
			      [=](auto dst_ptr) {
				do_set(dst_ptr, offset, value, n_elems);
			      });
      }

      virtual void set_value( int64_t dst_data, Datatype::Enum dst_type, int64_t offset, uint8_t value, int64_t n_elems ) {
	set_buffer_value(dst_data, dst_type, offset, value, n_elems);
      }
      virtual void set_value( int64_t dst_data, Datatype::Enum dst_type, int64_t offset, int16_t value, int64_t n_elems ) {
	set_buffer_value(dst_data, dst_type, offset, value, n_elems);
      }
      virtual void set_value( int64_t dst_data, Datatype::Enum dst_type, int64_t offset, int32_t value, int64_t n_elems ) {
	set_buffer_value(dst_data, dst_type, offset, value, n_elems);
      }
      virtual void set_value( int64_t dst_data, Datatype::Enum dst_type, int64_t offset, int64_t value, int64_t n_elems ) {
	set_buffer_value(dst_data, dst_type, offset, value, n_elems);
      }
      virtual void set_value( int64_t dst_data, Datatype::Enum dst_type, int64_t offset, float value, int64_t n_elems ) {
	set_buffer_value(dst_data, dst_type, offset, value, n_elems);
      }
      virtual void set_value( int64_t dst_data, Datatype::Enum dst_type, int64_t offset, double value, int64_t n_elems ) {
	set_buffer_value(dst_data, dst_type, offset, value, n_elems);
      }


      template<typename dst_type>
      dst_type get_buffer_value( int64_t src_data, Datatype::Enum src_type, int64_t offset )
      {
	return typed_buffer_op<dst_type>(src_data, src_type,
					 [=](auto src_ptr) {
					   return do_get<dst_type>(src_ptr, offset);
					 });
      }

      virtual uint8_t get_value_int8( int64_t src_data, Datatype::Enum src_type, int64_t offset ) {
	return get_buffer_value<uint8_t>( src_data, src_type, offset );
      }
      virtual int16_t get_value_int16( int64_t src_data, Datatype::Enum src_type, int64_t offset ) {
	return get_buffer_value<int16_t>( src_data, src_type, offset );
      }
      virtual int32_t get_value_int32( int64_t src_data, Datatype::Enum src_type, int64_t offset ) {
	return get_buffer_value<int32_t>( src_data, src_type, offset );
      }
      virtual int64_t get_value_int64( int64_t src_data, Datatype::Enum src_type, int64_t offset ) {
	return get_buffer_value<int64_t>( src_data, src_type, offset );
      }
      virtual float get_value_float( int64_t src_data, Datatype::Enum src_type, int64_t offset ) {
	return get_buffer_value<float>( src_data, src_type, offset );
      }
      virtual double get_value_double( int64_t src_data, Datatype::Enum src_type, int64_t offset ) {
	return get_buffer_value<double>( src_data, src_type, offset );
      }

      virtual void release_manager() {
	delete this;
      }
    };
  }
}
#endif
