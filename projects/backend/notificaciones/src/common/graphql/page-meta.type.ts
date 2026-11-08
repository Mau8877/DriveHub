import { Field, Int, ObjectType } from '@nestjs/graphql'

import type { PageMeta } from '../response/api-response.js'

/** La paginación de un listado: los mismos cuatro campos que el bloque `meta` de REST (ver RESPONSES_BACKEND.md). */
@ObjectType('PageMeta')
export class PageMetaType implements PageMeta {
  @Field(() => Int)
  page!: number

  @Field(() => Int)
  pageSize!: number

  @Field(() => Int)
  totalItems!: number

  @Field(() => Int)
  totalPages!: number
}
