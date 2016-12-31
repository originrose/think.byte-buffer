#ifndef BYTE_BUFFER_HPP
#define BYTE_BUFFER_HPP

namespace think { namespace byte_buffer {

    using namespace std;

    struct EndianType {
      enum Enum {
	LittleEndian = 1,
	BigEndian = 2,
      };
    };

    struct Datatype {
      enum Enum {
	Byte = 0,
	Short,
	Int,
	Long,
	Float,
	Double,
      };
    };

    struct TypedBuffer
    {
    public:
      const int64_t m_buffer;
      const int64_t m_length;
      const Datatype::Enum m_datatype;
      TypedBuffer( int64_t buf, int64_t len, Datatype::Enum dtype )
	: m_buffer( buf )
	, m_length (len)
	, m_datatype( dtype)
      {
      }
      TypedBuffer()
	: m_buffer( 0 )
	, m_length( 0 )
	, m_datatype( Datatype::Byte )
      {
      }
      TypedBuffer( const TypedBuffer& other )
	: m_buffer( other.m_buffer )
	, m_length( other.m_length )
	, m_datatype( other.m_datatype )
      {
      }
    };

    class BufferManager
    {
    public:
      virtual ~BufferManager(){}
      virtual TypedBuffer allocate( int64_t size, const char* file, int line ) = 0;
      virtual void release( int64_t data) = 0;

      virtual void copy( TypedBuffer src, int64_t src_offset,
			 char* dst, int64_t offset, int64_t n_elems ) = 0;
      virtual void copy( TypedBuffer src, int64_t src_offset,
			 short* dst, int64_t dst_offset, int64_t n_elems ) = 0;
      virtual void copy( TypedBuffer src, int64_t src_offset,
			 int32_t* dst, int64_t dst_offset, int64_t n_elems ) = 0;
      virtual void copy( TypedBuffer src, int64_t src_offset,
			 int64_t* dst, int64_t dst_offset, int64_t n_elems ) = 0;
      virtual void copy( TypedBuffer src, int64_t src_offset,
			 float* dst, int64_t dst_offset, int64_t n_elems ) = 0;
      virtual void copy( TypedBuffer src, int64_t src_offset,
			 double* dst, int64_t dst_offset, int64_t n_elems ) = 0;

      virtual void copy( const char* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) = 0;
      virtual void copy( const short* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) = 0;
      virtual void copy( const int32_t* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) = 0;
      virtual void copy( const int64_t* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) = 0;
      virtual void copy( const float* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) = 0;
      virtual void copy( const double* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) = 0;


      virtual void copy( TypedBuffer src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) = 0;


      virtual void set_value( TypedBuffer dst, int64_t offset, char value, int64_t n_elems ) = 0;
      virtual void set_value( TypedBuffer dst, int64_t offset, short value, int64_t n_elems ) = 0;
      virtual void set_value( TypedBuffer dst, int64_t offset, int32_t value, int64_t n_elems ) = 0;
      virtual void set_value( TypedBuffer dst, int64_t offset, int64_t value, int64_t n_elems ) = 0;
      virtual void set_value( TypedBuffer dst, int64_t offset, float value, int64_t n_elems ) = 0;
      virtual void set_value( TypedBuffer dst, int64_t offset, double value, int64_t n_elems ) = 0;

      virtual char get_value_char( TypedBuffer src, int64_t offset ) = 0;
      virtual short get_value_short( TypedBuffer src, int64_t offset ) = 0;
      virtual int32_t get_value_int32_t( TypedBuffer src, int64_t offset ) = 0;
      virtual int64_t get_value_int64( TypedBuffer src, int64_t offset ) = 0;
      virtual float get_value_float( TypedBuffer src, int64_t offset ) = 0;
      virtual double get_value_double( TypedBuffer src, int64_t offset ) = 0;


      static BufferManager* create_buffer_manager();
      virtual void release_manager() = 0;
    };
}}

#endif
