// Copyright (c) 2016-2020 Dr. Colin Hirsch and Daniel Frey
// Please see LICENSE for license or visit https://github.com/taocpp/PEGTL/

#ifndef TAO_PEGTL_INTERNAL_ISTREAM_READER_HPP
#define TAO_PEGTL_INTERNAL_ISTREAM_READER_HPP

#include <istream>

#if !defined( __cpp_exceptions )
#include <cstdio>
#include <exception>
#endif

#include "../config.hpp"
#include "../input_error.hpp"

namespace tao
{
   namespace TAO_PEGTL_NAMESPACE
   {
      namespace internal
      {
         struct istream_reader
         {
            explicit istream_reader( std::istream& s ) noexcept
               : m_istream( s )
            {
            }

            std::size_t operator()( char* buffer, const std::size_t length )
            {
               m_istream.read( buffer, std::streamsize( length ) );

               if( const auto r = m_istream.gcount() ) {
                  return std::size_t( r );
               }
               if( m_istream.eof() ) {
                  return 0;
               }
#if defined( __cpp_exceptions )
               TAO_PEGTL_THROW_INPUT_ERROR( "error in istream.read()" );
#else
               std::fputs( "std::istream::read() failed\n", stderr );
               std::terminate();
#endif
            }

            std::istream& m_istream;
         };

      }  // namespace internal

   }  // namespace TAO_PEGTL_NAMESPACE

}  // namespace tao

#endif
