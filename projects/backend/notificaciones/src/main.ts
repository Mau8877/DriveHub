import { ConsoleLogger, ValidationPipe } from '@nestjs/common'
import { NestFactory } from '@nestjs/core'

import { AppModule } from './app.module.js'
import { GlobalExceptionFilter } from './common/filters/global-exception.filter.js'

async function bootstrap() {
  // * Logs en JSON estructurado, igual que logstash-logback-encoder en los
  // * servicios Spring (ver LOGGING_BACKEND.md).
  const app = await NestFactory.create(AppModule, {
    logger: new ConsoleLogger({ json: true }),
  })
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }))
  app.useGlobalFilters(new GlobalExceptionFilter())
  app.enableShutdownHooks()
  await app.listen(process.env.PORT ?? 8086)
}
await bootstrap()
