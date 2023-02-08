// Copyright (c) 2014-2020 Dr. Colin Hirsch and Daniel Frey
// Please see LICENSE for license or visit https://github.com/taocpp/PEGTL/

#ifndef TAO_PEGTL_POSITION_HPP
#define TAO_PEGTL_POSITION_HPP

#include <cstdlib>
#include <string>
#include <utility>

#include "config.hpp"

#include "internal/iterator.hpp"

namespace tao
{
   namespace TAO_PEGTL_NAMESPACE
   {
      struct position
      {
         template< typename T >
         position( const internal::iterator& in_iter, T&& in_source )
            : byte( in_iter.byte ),
              line( in_iter.line ),
              byte_in_line( in_iter.byte_in_line ),
              source( std::forward< T >( in_source ) )
         {
         }

         std::size_t byte;
         std::size_t line;
         std::size_t byte_in_line;
         std::string source;
      };

      inline std::string to_string( const position& p )
      {
         return p.source + ':' + std::to_string(p.line) + ':' + std::to_string(p.byte_in_line) + '(' + std::to_string(p.byte) + ')';
      }

   }  // namespace TAO_PEGTL_NAMESPACE

}  // namespace tao

#endif
