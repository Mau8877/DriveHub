import { ConsoleLogger, ValidationPipe } from '@nestjs/common'
import { NestFactory } from '@nestjs/core'
import type { NestExpressApplication } from '@nestjs/platform-express'

import { AppModule } from './app.module.js'
import { GlobalExceptionFilter } from './common/filters/global-exception.filter.js'

async function bootstrap() {
  // * Logs en JSON estructurado, igual que logstash-logback-encoder en los
  // * servicios Spring (ver LOGGING_BACKEND.md).
  const app = await NestFactory.create<NestExpressApplication>(AppModule, {
    logger: new ConsoleLogger({ json: true }),
  })
  // Express 5 trae el parser simple: sin este, filter[campo] no llega como objeto (ver ENDPOINTS_BACKEND.md)
  app.set('query parser', 'extended')
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }))
  app.useGlobalFilters(new GlobalExceptionFilter())
  app.enableShutdownHooks()
  await app.listen(process.env.PORT ?? 8086)
}
await bootstrap()
