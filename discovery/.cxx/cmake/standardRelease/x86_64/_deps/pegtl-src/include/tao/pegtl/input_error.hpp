// Copyright (c) 2014-2020 Dr. Colin Hirsch and Daniel Frey
// Please see LICENSE for license or visit https://github.com/taocpp/PEGTL/

#ifndef TAO_PEGTL_INPUT_ERROR_HPP
#define TAO_PEGTL_INPUT_ERROR_HPP

#include <cerrno>
#include <string>
#include <stdexcept>

#include "config.hpp"

namespace tao
{
   namespace TAO_PEGTL_NAMESPACE
   {
      struct input_error
         : std::runtime_error
      {
         input_error( const std::string& message, const int in_errorno )
            : std::runtime_error( message ),
              errorno( in_errorno )
         {
         }

         int errorno;
      };

   }  // namespace TAO_PEGTL_NAMESPACE

}  // namespace tao

#define TAO_PEGTL_INTERNAL_UNWRAP( ... ) __VA_ARGS__

#define TAO_PEGTL_THROW_INPUT_ERROR( MESSAGE )                                          \
   do {                                                                                 \
      const int errorno = errno;                                                        \
      std::string mess = "pegtl: " + std::string(TAO_PEGTL_INTERNAL_UNWRAP( MESSAGE )) + " errno " + std::to_string(errorno); \
      throw tao::TAO_PEGTL_NAMESPACE::input_error( mess, errorno );                \
   } while( false )

#define TAO_PEGTL_THROW_INPUT_WIN32_ERROR( MESSAGE )                                             \
   do {                                                                                          \
      const int errorno = GetLastError();                                                        \
      std::string mess = "pegtl: " + std::string(TAO_PEGTL_INTERNAL_UNWRAP( MESSAGE )) + " GetLastError() " + std::to_string(errorno); \
      throw tao::TAO_PEGTL_NAMESPACE::input_error( mess, errorno );                         \
   } while( false )

#endif
